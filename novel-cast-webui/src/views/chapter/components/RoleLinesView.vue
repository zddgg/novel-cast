<template>
  <div class="container">
    <div v-if="!roles && !linesMappings">
      <div style="text-align: center; margin-bottom: 20px">
        <a-space size="large">
          <span style="font-size: 16px">还没有AI分析结果</span>
          <a-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="loading && aiResultError"
            @click="handleAiInference"
            >点击生成</a-button
          >
          <a-button
            size="large"
            :loading="loading"
            :disabled="loading"
            @click="handleIgnoreAiResult"
          >
            跳过AI分析
          </a-button>
        </a-space>
      </div>
      <div v-if="loading || aiResultError">
        <span style="font-size: 16px">{{ aiResultText }}</span>
      </div>
    </div>
    <a-space
      v-if="(roles || linesMappings) && !aiResultError"
      direction="vertical"
      style="width: 100%"
      size="large"
    >
      <a-card :bordered="false" :body-style="{ padding: '0' }" title="角色分析">
        <a-table
          :columns="rolesColumns"
          :data="roles"
          :pagination="false"
          :size="'large'"
          :bordered="{ cell: true }"
          column-resizable
        >
          <template #operations="{ record }">
            <a-space size="large">
              <a-button type="primary" @click="markedLinesForRole(record.role)">
                标记台词
              </a-button>
              <a-popconfirm
                v-if="!record.backup"
                content="这个角色的台词怎么处理?"
                type="error"
                ok-text="去合并"
                cancel-text="不处理"
                @ok="handleConfirmOk(record)"
              >
                <a-button
                  type="primary"
                  status="danger"
                  :disabled="
                    roles.map((item) => item.backup).includes(record.role)
                  "
                >
                  删除角色
                </a-button>
              </a-popconfirm>
              <a-button
                v-else
                type="primary"
                status="warning"
                @click="undoDelete(record)"
              >
                撤销删除
              </a-button>
            </a-space>
          </template>
        </a-table>
        <div>
          <a-space style="margin-top: 20px; float: right" size="large">
            <a-button
              v-if="hasRoles"
              type="primary"
              status="danger"
              size="large"
              @click="saveAndNext"
              >保存并选择模型
            </a-button>
            <a-button
              v-if="hasRoles"
              type="primary"
              size="large"
              status="warning"
              @click="handleUpdateRoles"
              >保存
            </a-button>
            <a-button type="primary" size="large" @click="next"
              >选择模型</a-button
            >
            <a-button size="large" @click="close">关闭</a-button>
          </a-space>
        </div>
      </a-card>
      <a-divider />
      <a-card :bordered="false" :body-style="{ padding: '0' }" title="台词分析">
        <a-table
          :columns="linesMappingsColumns"
          :data="linesMappings"
          :pagination="false"
          :size="'large'"
          :bordered="{ cell: true }"
          column-resizable
        >
          <template #id="{ rowIndex }">
            {{ rowIndex + 1 }}
          </template>
          <template #operations="{ record }">
            <a-space>
              <a-button type="primary" @click="markedLines(record.linesIndex)">
                标记台词
              </a-button>
            </a-space>
          </template>
        </a-table>
      </a-card>
    </a-space>
    <div style="margin-bottom: 60px">
      <a-space style="margin-top: 20px; float: right" size="large">
        <a-button type="primary" size="large" @click="next">选择模型</a-button>
        <a-button size="large" @click="close">关闭</a-button>
      </a-space>
    </div>
    <a-modal
      v-model:visible="visible"
      :title="`删除角色: ${currentRoleRecord.role}`"
      @cancel="handleCancel"
      @before-ok="handleBeforeOk"
    >
      <a-form :model="currentRoleRecord">
        <a-form-item field="backup" label="合并到">
          <a-select v-model="currentRoleRecord.backup">
            <a-option
              v-for="(item, index) in roles?.filter(
                (item1) =>
                  !item1.backup && item1.role !== currentRoleRecord.role
              )"
              :key="index"
              >{{ item.role }}
            </a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';
  import { Message } from '@arco-design/web-vue';
  import useLoading from '@/hooks/loading';
  import { FetchStream, IFetchStreamOptions } from '@/api/stream';
  import {
    ignoreAiResult,
    LinesMapping,
    queryAiResult,
    Role,
    updateRoles,
  } from '@/api/chapter';
  import { TableColumnData } from '@arco-design/web-vue/es/table/interface';

  const route = useRoute();
  const props = defineProps({
    roleLinesViewVisible: {
      type: Boolean,
      default: false,
    },
    modelSelectedViewVisible: {
      type: Boolean,
      default: false,
    },
    chapterName: {
      type: String,
    },
  });

  const emits = defineEmits([
    'update:roleLinesViewVisible',
    'update:modelSelectedViewVisible',
    'closeDrawerFetchData',
    'linesPointerForRole',
    'linesPointer',
  ]);

  const { loading, setLoading } = useLoading();

  const aiResultError = ref(false);

  const roles = ref<Role[]>([]);

  const linesMappings = ref<LinesMapping[]>([]);

  const aiResultText = ref('');

  const rolesColumns = ref<TableColumnData[]>([
    {
      title: '角色',
      dataIndex: 'role',
    },
    {
      title: '性别',
      dataIndex: 'gender',
    },
    {
      title: '年龄段',
      dataIndex: 'ageGroup',
    },
    {
      title: '合并至',
      dataIndex: 'backup',
    },
    {
      title: '操作',
      dataIndex: 'operations',
      slotName: 'operations',
    },
  ]);

  const linesMappingsColumns = ref<TableColumnData[]>([
    {
      title: '序号',
      slotName: 'id',
    },
    {
      title: '文章中位置',
      dataIndex: 'linesIndex',
    },
    {
      title: '角色',
      dataIndex: 'role',
    },
    {
      title: '性别',
      dataIndex: 'gender',
    },
    {
      title: '年龄段',
      dataIndex: 'ageGroup',
    },
    {
      title: '情感',
      dataIndex: 'mood',
    },
    {
      title: '操作',
      dataIndex: 'operations',
      slotName: 'operations',
    },
  ]);

  const currentRoleRecord = ref<Role>({} as Role);

  const hasRoles = computed(() => {
    return !!roles.value && roles.value.length > 0;
  });

  const markedLines = (index: string) => {
    emits('linesPointer', index);
  };

  const markedLinesForRole = (role: string) => {
    const indexArr = linesMappings.value
      .filter((item) => item.role === role)
      .map((item) => item.linesIndex);
    emits('linesPointerForRole', indexArr);
  };

  const close = () => {
    emits('update:modelSelectedViewVisible', true);
    emits('linesPointer', undefined);
    emits('linesPointer', undefined);
    emits('closeDrawerFetchData');
  };

  const visible = ref(false);

  const resetCurrentRoleRecord = () => {
    currentRoleRecord.value = {} as Role;
  };

  const handleBeforeOk = (done: (closed: boolean) => void) => {
    const { role, backup } = currentRoleRecord.value;

    if (backup) {
      const findIndex = roles.value.findIndex((item) => item.role === role);
      if (findIndex) {
        const item = roles.value.splice(findIndex, 1);
        item[0].backup = backup;
        roles.value.push(item[0]);
      }
      done(true);
      resetCurrentRoleRecord();
    } else {
      done(false);
    }
  };

  const handleCancel = () => {
    resetCurrentRoleRecord();
    visible.value = false;
  };

  const handleConfirmOk = (record: Role) => {
    currentRoleRecord.value.role = record.role;
    currentRoleRecord.value.gender = record.gender;
    currentRoleRecord.value.ageGroup = record.ageGroup;
    visible.value = true;
  };

  const undoDelete = (record: Role) => {
    record.backup = '';
  };

  const handleUpdateRoles = async () => {
    const { msg } = await updateRoles({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
      roles: roles.value,
    });
    Message.success(msg);
  };

  const getAiResultData = async () => {
    const { data } = await queryAiResult({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    });
    roles.value = data?.roles;
    linesMappings.value = data?.linesMappings;
  };

  // 定义回调函数
  const handleMessage = (data: string[], index: number) => {
    aiResultText.value += data.join('');
    // 处理接收到的数据块
    console.log('Received data chunk', data.join(''), 'at index', index);
  };

  const handleDone = () => {
    if (aiResultText.value.endsWith('error')) {
      aiResultError.value = true;
    } else {
      setTimeout(() => {
        getAiResultData();
      }, 500);
    }
    setLoading(false);
  };

  const handleError = (response: Response) => {
    setLoading(false);
    // 请求出错的处理逻辑
    console.error('Request failed', response);
  };

  const handleTimeout = () => {
    setLoading(false);
    // 请求超时的处理逻辑
    console.error('Request timed out');
  };

  const handleAiInference = async () => {
    try {
      setLoading(true);
      aiResultText.value = '';
      // 创建 FetchStream 实例并发送请求
      const fetchOptions: IFetchStreamOptions = {
        url: '/api/chapter/aiInference',
        requestInit: {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            project: route.query.project as string,
            chapterName: props.chapterName as string,
          }),
        },
        onMessage: handleMessage,
        onDone: handleDone,
        onError: handleError,
        onTimeout: handleTimeout,
      };
      const fetchStream = new FetchStream(fetchOptions);

      fetchStream.startRequest();
    } catch {
      setLoading(false);
      Message.error('请求失败');
    }
  };

  const handleIgnoreAiResult = async () => {
    await ignoreAiResult({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    });
  };

  const closeAndNext = () => {
    emits('update:roleLinesViewVisible', false);
    emits('update:modelSelectedViewVisible', true);
  };

  const saveAndNext = () => {
    handleUpdateRoles().then(() => {
      closeAndNext();
    });
  };

  const next = () => {
    closeAndNext();
  };

  watch(
    () => props.chapterName,
    () => {
      if (props.chapterName) {
        getAiResultData();
      }
    },
    { immediate: true }
  );
</script>

<style scoped lang="less">
  .container {
    padding: 0 20px;
  }
</style>
