<template>
  <div class="container">
    <a-form :model="form" @submit="handleSubmit">
      <a-form-item field="project" label="项目名称" required>
        <a-input
          v-model="form.project"
          placeholder="please enter your project..."
        />
      </a-form-item>
      <a-form-item label="小说文本">
        <a-upload
          :auto-upload="false"
          :limit="1"
          draggable
          @change="onChange"
        />
      </a-form-item>
      <a-form-item>
        <a-button
          size="large"
          type="primary"
          html-type="submit"
          :loading="loading"
          >创建项目</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { FileItem } from '@arco-design/web-vue/es/upload/interfaces';
  import { createProject } from '@/api/project';
  import useLoading from '@/hooks/loading';

  defineProps({
    visible: {
      type: Boolean,
      default: false,
    },
  });

  const emits = defineEmits(['update:visible']);
  const { loading, setLoading } = useLoading();

  const form = ref({
    project: '',
  });

  const file = ref<FileItem>();

  const onChange = (fileList: FileItem[], fileItem: FileItem) => {
    file.value = {
      ...fileItem,
    };
  };

  const handleSubmit = async () => {
    try {
      setLoading(true);
      const formData = new FormData();
      formData.append('project', form.value.project);
      formData.append('file', file.value?.file as Blob);
      await createProject(formData);
      emits('update:visible', false);
    } finally {
      setLoading(false);
    }
  };
</script>

<style scoped>
  .container {
    padding: 20px;
  }

  .container:deep(.arco-upload-list-item .arco-upload-progress) {
    display: none;
  }
</style>
