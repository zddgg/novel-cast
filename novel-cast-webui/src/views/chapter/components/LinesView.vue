<template>
  <div class="container">
    <a-button
      style="margin-bottom: 20px"
      :loading="loading"
      type="primary"
      size="large"
      @click="handleReCreateLines"
      >重新生成
    </a-button>
    <a-table
      :columns="columns"
      :data="linesList"
      :pagination="false"
      :size="'large'"
      :bordered="{ cell: true }"
      column-resizable
    >
      <template #id="{ rowIndex }">
        {{ rowIndex + 1 }}
      </template>
      <template #lines="{ record }">
        <a-textarea
          v-model="record.lines"
          :auto-size="{
            minRows: 1,
          }"
        />
      </template>
      <template #operations="{ record }">
        <a-space>
          <a-button type="primary" @click="markedLines(record.index)">
            标记台词
          </a-button>
          <a-button
            v-if="record.delFlag"
            type="primary"
            status="warning"
            @click="() => (record.delFlag = false)"
          >
            撤销
          </a-button>
          <a-button
            v-else
            type="primary"
            status="danger"
            @click="() => (record.delFlag = true)"
          >
            删除
          </a-button>
        </a-space>
      </template>
    </a-table>
    <div style="margin-bottom: 60px">
      <a-space style="margin-top: 20px; float: right" size="large">
        <a-button
          v-if="hasLines"
          :loading="loading"
          type="primary"
          status="success"
          size="large"
          @click="handleLinesUpdate"
          >保存
        </a-button>
        <a-button type="primary" size="large" @click="closeAndNext"
          >下一步</a-button
        >
        <a-button size="large" @click="close">关闭</a-button>
      </a-space>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import {
    ChapterParams,
    Lines,
    linesUpdate,
    queryLines,
    reCreateLines,
  } from '@/api/chapter';
  import { useRoute } from 'vue-router';
  import { TableColumnData } from '@arco-design/web-vue/es/table/interface';
  import useLoading from '@/hooks/loading';
  import { Message } from '@arco-design/web-vue';

  const props = defineProps({
    linesViewVisible: {
      type: Boolean,
      default: false,
    },
    modelViewVisible: {
      type: Boolean,
      default: false,
    },
    chapterName: {
      type: String,
    },
  });

  const route = useRoute();
  const emits = defineEmits([
    'update:linesViewVisible',
    'update:modelViewVisible',
    'closeDrawerFetchData',
    'linesPointer',
  ]);

  const { loading, setLoading } = useLoading();

  const close = () => {
    emits('linesPointer', undefined);
    emits('closeDrawerFetchData');
  };

  const columns = ref<TableColumnData[]>([
    {
      title: '序号',
      slotName: 'id',
      width: 100,
    },
    {
      title: '文章中位置',
      dataIndex: 'index',
      width: 150,
    },
    {
      title: '台词',
      dataIndex: 'lines',
      slotName: 'lines',
    },
    {
      title: '操作',
      dataIndex: 'operations',
      slotName: 'operations',
      width: 150,
    },
  ]);

  const linesList = ref<Lines[]>([]);

  const hasLines = computed(() => {
    return !!linesList.value && linesList.value.length > 0;
  });

  const markedLines = (index: string) => {
    emits('linesPointer', index);
  };

  const getLinesData = async () => {
    const { data } = await queryLines({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    } as ChapterParams);
    linesList.value = data;
  };

  const handleLinesUpdate = async () => {
    try {
      setLoading(true);
      const { msg } = await linesUpdate({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
        linesList: linesList.value,
      } as ChapterParams);
      await getLinesData();
      Message.success(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleReCreateLines = async () => {
    try {
      setLoading(true);
      const { msg } = await reCreateLines({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
      } as ChapterParams);
      await getLinesData();
      Message.success(msg);
    } finally {
      setLoading(false);
    }
  };

  const closeAndNext = () => {
    emits('update:linesViewVisible', false);
    emits('update:modelViewVisible', true);
  };

  watch(
    () => props.chapterName,
    () => {
      if (props.chapterName) {
        getLinesData();
      }
    },
    { immediate: true }
  );
</script>

<style scoped>
  .container {
    padding: 0 20px;
  }
</style>
