<template>
  <div class="container">
    <a-row>
      <a-col :span="10">
        <a-form-item
          label="角色台词修饰符"
          help="右边选不到可以在下拉框中手动填，紧密相连的一组符号，可配置多种，多层次只匹配最外层"
        >
          <a-input v-model="computedLinesModifiers" readonly />
        </a-form-item>
      </a-col>
      <a-col :span="10">
        <a-form-item>
          <a-select
            v-model="linesModifiers"
            multiple
            allow-create
            :options="modifiersList"
          />
        </a-form-item>
      </a-col>
      <a-col :span="4">
        <a-form-item>
          <a-button :loading="loading" type="primary" @click="handleParseLines"
            >台词解析
          </a-button>
        </a-form-item>
      </a-col>
    </a-row>

    <a-table
      :columns="columns"
      :data="linesList"
      :pagination="false"
      :size="'large'"
      :bordered="{ cell: true }"
      column-resizable
      @row-click="rowClick"
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
    parseLines,
    queryLines,
  } from '@/api/chapter';
  import { useRoute } from 'vue-router';
  import {
    TableColumnData,
    TableData,
  } from '@arco-design/web-vue/es/table/interface';
  import useLoading from '@/hooks/loading';
  import { Message } from '@arco-design/web-vue';

  const props = defineProps({
    chapterName: {
      type: String,
    },
  });

  const route = useRoute();
  const emits = defineEmits(['closeDrawerFetchData', 'linesPointer']);

  const { loading, setLoading } = useLoading();

  const rowClick = (record: TableData) => {
    emits('linesPointer', record.index);
  };

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
      width: 100,
    },
  ]);

  const modifiersList = [
    {
      label: '“中文双引号”',
      value: '“”',
    },
    {
      label: '"英文双引号"',
      value: '""',
    },
    {
      label: '‘中文单引号’',
      value: '‘’',
    },
    {
      label: "'英文单引号'",
      value: "''",
    },
    {
      label: '（中文小括号）',
      value: '（）',
    },
    {
      label: '(英文小括号)',
      value: '()',
    },
    {
      label: '【中文中括号】',
      value: '【】',
    },
    {
      label: '[英文中括号]',
      value: '[]',
    },
    {
      label: '{英文大括号}',
      value: '{}',
    },
    {
      label: '「中文竖直角单引号」',
      value: '「」',
    },
    {
      label: '⌈英文竖直角单引号⌋',
      value: '⌈⌋',
    },
  ];

  const linesModifiers = ref<string[]>([modifiersList[0].value]);

  const computedLinesModifiers = computed(() => {
    return linesModifiers.value.join(',');
  });

  const linesList = ref<Lines[]>([]);

  const hasLines = computed(() => {
    return !!linesList.value && linesList.value.length > 0;
  });

  const getLinesData = async () => {
    const { data } = await queryLines({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    } as ChapterParams);
    linesList.value = data.linesList;
    linesModifiers.value = data.linesModifiers;
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

  const handleParseLines = async () => {
    try {
      setLoading(true);
      const { msg } = await parseLines({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
        linesModifiers: linesModifiers.value,
      } as any);
      await getLinesData();
      Message.success(msg);
    } finally {
      setLoading(false);
    }
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
