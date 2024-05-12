<template>
  <div class="container">
    <a-space size="large" direction="vertical" style="width: 100%">
      <div style="text-align: right">
        <a-space size="large">
          <span v-if="!aiProcess && !aiIgnore" style="font-size: 16px"
            >还没有AI分析结果</span
          >
          <div v-if="aiProcess">
            <a-space size="large">
              <a-popconfirm type="warning" @ok="handleAiReInference">
                <a-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  :disabled="loading && aiResultError"
                  >重新生成</a-button
                >
                <template #content>
                  <span>重新生成不会删除现有数据<br /></span>
                  <span>后面保存结果才会更新数据</span>
                </template>
              </a-popconfirm>
              <a-popconfirm type="warning" @ok="handleSaveAiReInferenceResult">
                <a-button
                  v-if="aiReInferenceFlag"
                  type="primary"
                  status="danger"
                  size="large"
                  :loading="loading"
                  :disabled="loading && aiResultError"
                  >保存此次生成结果</a-button
                >
                <template #content>
                  <span>保存后会重置本页面的所有模型配置</span>
                </template>
              </a-popconfirm>
            </a-space>
          </div>
          <a-button
            v-else
            type="primary"
            size="large"
            :loading="loading"
            :disabled="loading && aiResultError"
            @click="handleAiInference"
            >点击生成</a-button
          >
          <a-button
            v-if="!aiProcess && !aiIgnore"
            size="large"
            :loading="loading"
            :disabled="loading"
            @click="handleIgnoreAiResult"
          >
            跳过AI分析
          </a-button>
        </a-space>
        <div v-if="loading || aiResultError">
          <span style="font-size: 16px">{{ aiResultText }}</span>
        </div>
      </div>
      <!--   就是这里出问题   -->
      <a-form
        size="large"
        :model="{}"
        :label-col-props="{ span: 8 }"
        :wrapper-col-props="{ span: 16 }"
      >
        <a-space size="large" direction="vertical" style="width: 100%">
          <a-card title="公共角色配置" :body-style="{ padding: '0' }">
            <a-card
              v-for="(item, index) in commonRoleConfigs"
              :key="index"
              @click="showLightTextList(item.role.role)"
            >
              <a-row>
                <a-col :span="8">
                  <a-form-item label="角色">
                    <a-input v-model="item.role.role" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="语音模型">
                    <a-cascader
                      v-model="item.tmpModels"
                      multiple
                      path-mode
                      :options="speechModelData"
                      @change="(value) => {
                        item.models = (value as []).map((item1) => {
                          return {group: item1[0], name: item1[1]}
                        })
                      }"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="模型策略">
                    <a-select v-model="item.strategyType">
                      <a-option>固定</a-option>
                      <a-option>顺序</a-option>
                      <a-option>随机</a-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="语音情感">
                    <a-select v-model="item.moods" multiple> </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-card>
          </a-card>
          <a-card title="角色配置" :body-style="{ padding: '0' }">
            <a-card
              v-for="(item, index) in roleConfigs"
              :key="index"
              @click="showLightTextList(item.role.role)"
            >
              <a-row>
                <a-col :span="8">
                  <a-form-item label="角色">
                    <a-input v-model="item.role.role" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="性别">
                    <a-input v-model="item.role.gender" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="年龄段">
                    <a-input v-model="item.role.ageGroup" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="语音模型">
                    <a-cascader
                      v-model="item.tmpModels"
                      multiple
                      path-mode
                      :options="speechModelData"
                      @change="(value) => {
                        item.models = (value as []).map((item1) => {
                          return {group: item1[0], name: item1[1]}
                        })
                      }"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="模型策略">
                    <a-select v-model="item.strategyType">
                      <a-option>固定</a-option>
                      <a-option>顺序</a-option>
                      <a-option>随机</a-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="语音情感">
                    <a-select v-model="item.moods" multiple> </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row
                v-if="!['标题', '旁白'].includes(item.role.role)"
                style="display: flex; justify-content: right"
              >
                <a-space size="large">
                  <a-popconfirm
                    v-if="!item.role.backup"
                    content="这个角色的台词怎么处理?"
                    type="error"
                    ok-text="合并到其他角色"
                    cancel-text="当旁白处理"
                    @ok="handleConfirmOk(item.role)"
                  >
                    <a-button
                      type="primary"
                      status="danger"
                      :disabled="
                        (!!item.role.backup &&
                          roleConfigs
                            .map((item1) => item1.role.backup)
                            .includes(item.role.backup)) ||
                        aiReInferenceFlag
                      "
                    >
                      删除角色
                    </a-button>
                  </a-popconfirm>
                  <a-button
                    v-else
                    type="primary"
                    status="warning"
                    @click="() => (item.role.backup = '')"
                  >
                    撤销删除
                  </a-button>
                </a-space>
              </a-row>
            </a-card>
          </a-card>
          <a-card title="台词配置" :body-style="{ padding: 0 }">
            <a-card
              v-for="(item, index) in linesConfigs"
              :key="index"
              @click="showLightText(item.linesMapping.linesIndex)"
            >
              <a-row>
                <a-col :span="12">
                  <a-row>
                    <a-col :span="12">
                      <a-form-item label="角色">
                        <a-input v-model="item.linesMapping.role" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="情感">
                        <a-input v-model="item.linesMapping.mood" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="语音模型">
                        <a-cascader
                          v-model="item.tmpModel"
                          :options="speechModelData"
                          path-mode
                          @change="(value) => {
                            item.model = {
                              group: (value as string[])[0],
                              name: (value as string[])[1],
                            }
                          }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="语音情感">
                        <a-select
                          v-model="item.mood"
                          :options="computedMoods(item.model) as string[]"
                        ></a-select>
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-col>
                <a-col :span="12">
                  <a-form-item
                    label="台词"
                    :label-col-props="{ span: 4 }"
                    :wrapper-col-props="{ span: 20 }"
                  >
                    <a-textarea
                      v-model="item.linesMapping.lines"
                      :auto-size="{
                        minRows: 1,
                      }"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-card>
          </a-card>
        </a-space>
      </a-form>
      <div style="margin-bottom: 60px">
        <a-space style="margin-top: 20px; float: right" size="large">
          <a-button
            type="primary"
            status="success"
            size="large"
            :loading="loading"
            :disabled="!aiProcess || aiReInferenceFlag"
            @click="handleUpdateModelConfig"
            >保存
          </a-button>
          <a-button size="large" @click="close">关闭</a-button>
        </a-space>
      </div>
    </a-space>
    <a-modal
      v-model:visible="roleDeleteModalVisible"
      :title="`删除角色: ${currentRoleRecord.role}`"
      @cancel="handleCancel"
      @before-ok="handleBeforeOk"
    >
      <a-form :model="currentRoleRecord">
        <a-form-item field="backup" label="合并到">
          <a-select v-model="currentRoleRecord.backup">
            <a-option
              v-for="(item, index) in roleConfigs
                .map((item1) => item1.role)
                ?.filter(
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
  import { onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';
  import { CascaderOption, Message } from '@arco-design/web-vue';
  import useLoading from '@/hooks/loading';
  import { FetchStream, IFetchStreamOptions } from '@/api/stream';
  import {
    aiResultFormat,
    ignoreAiResult,
    LinesConfig,
    ModelConfig,
    ModelItem,
    queryModelConfig,
    Role,
    RoleModelConfig,
    saveAiReInferenceResult,
    updateModelConfig,
  } from '@/api/chapter';
  import { querySpeechModels, SpeechModelGroup } from '@/api/model';

  const route = useRoute();
  const props = defineProps({
    chapterName: {
      type: String,
    },
  });

  const emits = defineEmits([
    'closeDrawerFetchData',
    'linesPointerForRole',
    'linesPointer',
  ]);

  const { loading, setLoading } = useLoading();

  const aiResultError = ref(false);

  const aiResultText = ref('');

  const currentRoleRecord = ref<Role>({} as Role);

  const roleDeleteModalVisible = ref(false);

  const resetCurrentRoleRecord = () => {
    currentRoleRecord.value = {} as Role;
  };

  // start

  const speechModelData = ref<CascaderOption[]>([]);
  const speechMood = ref<SpeechModelGroup[]>([]);

  const getSpeechModels = async () => {
    const { data } = await querySpeechModels();
    speechMood.value = data;
    speechModelData.value = data.map((item) => {
      return {
        value: item.group,
        children: item.speechModels.map((item1) => {
          return { value: item1.name };
        }),
      };
    });
  };

  const computedMoods = (modelItem: ModelItem) => {
    if (!modelItem || !modelItem.group || !modelItem.name) {
      return null;
    }
    return speechMood.value
      .flatMap((item) =>
        item.speechModels.map((model) => ({
          ...model,
          group: item.group,
        }))
      )
      .filter(
        (item) => modelItem.group === item.group && modelItem.name === item.name
      )
      .flatMap((item) => item.moods)
      .map((mood) => mood.name);
  };

  const commonRoleConfigs = ref<RoleModelConfig[]>([]);

  const roleConfigs = ref<RoleModelConfig[]>([]);

  const linesConfigs = ref<LinesConfig[]>([]);

  const aiProcess = ref<boolean>(false);
  const aiIgnore = ref<boolean>(false);
  const hasSpeechConfig = ref<boolean>(false);
  const aiReInferenceFlag = ref<boolean>(false);

  const showLightText = (linesIndex: string) => {
    emits('linesPointer', linesIndex);
  };

  const showLightTextList = (role: string) => {
    const indexArr = linesConfigs.value
      .map((item) => item.linesMapping)
      .filter((item) => item.role === role)
      .map((item) => item.linesIndex);
    emits('linesPointerForRole', indexArr);
  };

  const handleBeforeOk = (done: (closed: boolean) => void) => {
    const { role, backup } = currentRoleRecord.value;

    if (backup) {
      const findIndex = roleConfigs.value.findIndex(
        (item) => item.role.role === role
      );
      if (findIndex) {
        const item = roleConfigs.value.splice(findIndex, 1);
        item[0].role.backup = backup;
        roleConfigs.value.push(item[0]);

        linesConfigs.value.forEach((item1, index) => {
          if (item1.linesMapping.role === role) {
            linesConfigs.value[index].backup = role;
            linesConfigs.value[index].linesMapping.role = backup;
          }
        });
      }
      done(true);
      resetCurrentRoleRecord();
    } else {
      done(false);
    }
  };

  const handleCancel = () => {
    resetCurrentRoleRecord();
    roleDeleteModalVisible.value = false;
  };

  const handleConfirmOk = (record: Role) => {
    currentRoleRecord.value.role = record.role;
    currentRoleRecord.value.gender = record.gender;
    currentRoleRecord.value.ageGroup = record.ageGroup;
    roleDeleteModalVisible.value = true;
  };

  const modelConfigDataParse = (data: ModelConfig) => {
    commonRoleConfigs.value = data.commonRoleConfigs.map(
      (item: RoleModelConfig) => {
        return {
          ...item,
          tmpModels: item.models?.map((item1) => {
            return [item1.group, item1.name];
          }),
        };
      }
    );
    roleConfigs.value = data.roleConfigs.map((item: RoleModelConfig) => {
      return {
        ...item,
        tmpModels: item.models?.map((item1) => {
          return [item1.group, item1.name];
        }),
      };
    });
    linesConfigs.value = data.linesConfigs.map((item: LinesConfig) => {
      let tmpModel: string[] = [];
      if (item.model) {
        tmpModel = [item.model.group, item.model.name];
      }
      return {
        ...item,
        tmpModel,
      };
    });
  };

  const getModelConfigData = async () => {
    const { data } = await queryModelConfig({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    });
    modelConfigDataParse(data);
    aiProcess.value = data.aiProcess;
    aiIgnore.value = data.aiIgnore;
    hasSpeechConfig.value = data.hasSpeechConfig;
  };

  const handleAiResultFormat = async () => {
    const { data } = await aiResultFormat({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
      aiResultText: aiResultText.value,
    });
    modelConfigDataParse(data);
  };

  // 定义回调函数
  const handleAiInferenceMessage = (data: string[], index: number) => {
    aiResultText.value += data.join('');
    // 处理接收到的数据块
    console.log('Received data chunk', data.join(''), 'at index', index);
  };

  const handleAiInferenceDone = () => {
    if (aiResultText.value.endsWith('error')) {
      aiResultError.value = true;
    } else if (aiReInferenceFlag.value) {
      handleAiResultFormat();
    } else {
      setTimeout(() => {
        getModelConfigData();
      }, 500);
    }
    setLoading(false);
  };

  const handleAiInferenceError = (response: Response) => {
    setLoading(false);
    // 请求出错的处理逻辑
    console.error('Request failed', response);
  };

  const handleAiInferenceTimeout = () => {
    setLoading(false);
    // 请求超时的处理逻辑
    console.error('Request timed out');
  };

  const aiInference = async (url: string) => {
    try {
      setLoading(true);
      aiResultText.value = '';
      // 创建 FetchStream 实例并发送请求
      const fetchOptions: IFetchStreamOptions = {
        url,
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
        onMessage: handleAiInferenceMessage,
        onDone: handleAiInferenceDone,
        onError: handleAiInferenceError,
        onTimeout: handleAiInferenceTimeout,
      };
      const fetchStream = new FetchStream(fetchOptions);

      fetchStream.startRequest();
    } catch {
      setLoading(false);
      Message.error('请求失败');
    }
  };

  const handleAiInference = async () => {
    await aiInference('/api/chapter/aiInference');
  };

  const handleAiReInference = async () => {
    aiReInferenceFlag.value = true;
    await aiInference('/api/chapter/aiReInference');
  };

  const saveModelConfig = async () => {
    return updateModelConfig({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
      modelConfig: {
        commonRoleConfigs: commonRoleConfigs.value,
        roleConfigs: roleConfigs.value,
        linesConfigs: linesConfigs.value,
      } as ModelConfig,
    });
  };

  const handleUpdateModelConfig = async () => {
    const { msg } = await saveModelConfig();
    await getModelConfigData();
    Message.success(msg);
  };

  const handleIgnoreAiResult = async () => {
    await ignoreAiResult({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    });
    aiProcess.value = true;
    aiIgnore.value = true;
  };

  const handleSaveAiReInferenceResult = async () => {
    await saveAiReInferenceResult({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
      aiResultText: aiResultText.value,
    });
    await getModelConfigData();
    aiReInferenceFlag.value = false;
  };

  const close = () => {
    emits('closeDrawerFetchData');
  };

  onMounted(() => {
    getSpeechModels();
  });

  watch(
    () => props.chapterName,
    () => {
      if (props.chapterName) {
        getModelConfigData();
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
