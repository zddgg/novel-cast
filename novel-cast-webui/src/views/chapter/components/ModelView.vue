<template>
  <div class="container">
    <a-space size="large" direction="vertical" style="width: 100%">
      <div>
        <div style="text-align: right">
          <a-space size="large">
            <a-button
              v-if="
                roleConfigs &&
                roleConfigs.length > 0 &&
                linesConfigs &&
                linesConfigs.length > 0
              "
              type="primary"
              @click="handleLoadProjectRoleModel"
            >
              加载预置角色模型
            </a-button>
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
                <a-popconfirm
                  type="warning"
                  @ok="handleSaveAiReInferenceResult"
                >
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
        </div>
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
                    <a-input v-model="item.role.role" readonly />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row>
                <a-col :span="8">
                  <a-form-item label="模型">
                    <a-cascader
                      v-model="item.tmpGsvModel"
                      path-mode
                      :options="gsvModelDataOptions"
                      @change="(value) => {
                        item.gsvModel = {
                          group:(value as string[])[0],
                          name:(value as string[])[1]
                        }
                      }"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="音色">
                    <a-cascader
                      v-model="item.tmpModel"
                      path-mode
                      :options="speechModelOptions"
                      @change="(value) => {
                            item.model = {
                              group:(value as string[])[0],
                              name:(value as string[])[1]
                            }
                          }"
                    >
                      <template #option="{ data }">
                        <div
                          v-if="data.isLeaf"
                          style="
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                          "
                        >
                          <span>{{ data.value }}</span>
                          <a-button
                            v-if="
                              activeAudio !== data.parent.key + '-' + data.value
                            "
                            type="outline"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="playAudio(data)"
                          >
                            <icon-play-arrow-fill :size="16" />
                          </a-button>
                          <a-button
                            v-else
                            type="outline"
                            status="danger"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="stopAudio"
                          >
                            <icon-mute-fill :size="16" />
                          </a-button>
                        </div>
                        <span v-else>
                          {{ data.value }}
                        </span>
                      </template>
                    </a-cascader>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="情感">
                    <a-select v-model="item.mood">
                      <a-option
                        v-for="(item1, index1) in computedMoods(
                          item.model?.group,
                          item.model?.name
                        )"
                        :key="index1"
                        :value="item1.name"
                      >
                        <div
                          style="
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            width: 100%;
                          "
                        >
                          <span>{{ item1.name }}</span>
                          <a-button
                            v-if="
                              activeAudio !==
                              `${item1.group}-${item1.audioName}-${item1.name}`
                            "
                            type="outline"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="
                              playMoodAudio(
                                `${item1.group}-${item1.audioName}-${item1.name}`,
                                item1.url
                              )
                            "
                          >
                            <icon-play-arrow-fill :size="16" />
                          </a-button>
                          <a-button
                            v-else
                            type="outline"
                            status="danger"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="stopAudio"
                          >
                            <icon-mute-fill :size="16" />
                          </a-button>
                        </div>
                      </a-option>
                    </a-select>
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
                    <a-input v-model="item.role.role" readonly />
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
                  <a-form-item label="模型">
                    <a-cascader
                      v-model="item.tmpGsvModel"
                      path-mode
                      :options="gsvModelDataOptions"
                      @change="(value) => {
                        item.gsvModel = {
                          group:(value as string[])[0],
                          name:(value as string[])[1]
                        }
                      }"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="音色">
                    <a-cascader
                      v-model="item.tmpModel"
                      path-mode
                      :options="speechModelOptions"
                      @change="(value) => {
                            item.model = {
                              group:(value as string[])[0],
                              name:(value as string[])[1]
                            }
                          }"
                    >
                      <template #option="{ data }">
                        <div
                          v-if="data.isLeaf"
                          style="
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                          "
                        >
                          <span>{{ data.value }}</span>
                          <a-button
                            v-if="
                              activeAudio !== data.parent.key + '-' + data.value
                            "
                            type="outline"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="playAudio(data)"
                          >
                            <icon-play-arrow-fill :size="16" />
                          </a-button>
                          <a-button
                            v-else
                            type="outline"
                            status="danger"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="stopAudio"
                          >
                            <icon-mute-fill :size="16" />
                          </a-button>
                        </div>
                        <span v-else>
                          {{ data.value }}
                        </span>
                      </template>
                    </a-cascader>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="情感">
                    <a-select v-model="item.mood">
                      <a-option
                        v-for="(item1, index1) in computedMoods(
                          item.model?.group,
                          item.model?.name
                        )"
                        :key="index1"
                        :value="item1.name"
                      >
                        <div
                          style="
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            width: 100%;
                          "
                        >
                          <span>{{ item1.name }}</span>
                          <a-button
                            v-if="
                              activeAudio !==
                              `${item1.group}-${item1.audioName}-${item1.name}`
                            "
                            type="outline"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="
                              playMoodAudio(
                                `${item1.group}-${item1.audioName}-${item1.name}`,
                                item1.url
                              )
                            "
                          >
                            <icon-play-arrow-fill :size="16" />
                          </a-button>
                          <a-button
                            v-else
                            type="outline"
                            status="danger"
                            size="mini"
                            style="margin-left: 10px"
                            @click.stop="stopAudio"
                          >
                            <icon-mute-fill :size="16" />
                          </a-button>
                        </div>
                      </a-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row style="display: flex; justify-content: right">
                <a-space size="large">
                  <a-popconfirm
                    v-if="!item.role.backup"
                    content="这个角色的台词怎么处理?"
                    type="error"
                    ok-text="合并到其他角色"
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
                        <a-input v-model="item.linesMapping.role" readonly />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="情感">
                        <a-input v-model="item.linesMapping.mood" readonly />
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
    <audio ref="audioElement" @ended="handleAudioEnded"></audio>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';
  import { CascaderOption, Message, Modal } from '@arco-design/web-vue';
  import useLoading from '@/hooks/loading';
  import { FetchStream, IFetchStreamOptions } from '@/api/stream';
  import {
    aiResultFormat,
    ignoreAiResult,
    LinesConfig,
    ModelConfig,
    queryModelConfig,
    Role,
    RoleModelConfig,
    saveAiReInferenceResult,
    updateModelConfig,
  } from '@/api/chapter';
  import {
    GsvModel,
    gsvModels,
    querySpeechModels,
    SpeechModelGroup,
  } from '@/api/model';
  import {
    checkProjectRoleModel,
    loadProjectRoleModel,
    setProjectRoleModel,
  } from '@/api/project';

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

  const audioElement = ref<HTMLAudioElement | null>(null); // ref 对象引用到 audio 元素

  const { loading, setLoading } = useLoading();

  const aiResultError = ref(false);

  const aiResultText = ref('');

  const currentRoleRecord = ref<Role>({} as Role);

  const roleDeleteModalVisible = ref(false);

  const resetCurrentRoleRecord = () => {
    currentRoleRecord.value = {} as Role;
  };

  // start

  const speechModelOptions = ref<CascaderOption[]>([]);
  const speechModelData = ref<SpeechModelGroup[]>([]);

  const getSpeechModels = async () => {
    const { data } = await querySpeechModels();
    speechModelData.value = data;
    speechModelOptions.value = data.map((item) => {
      return {
        value: item.group,
        children: item.speechModels.map((item1) => {
          return { value: item1.name };
        }),
      };
    });
  };

  const computedMoods = (group: string, name: string) => {
    if (group && name) {
      const groupValue = `${group}-${name}`;
      return speechModelData.value
        .flatMap((item) =>
          item.speechModels.map((model) => ({
            ...model,
            group: item.group,
          }))
        )
        .filter((item) => groupValue === `${item.group}-${item.name}`)
        .flatMap((item) =>
          item.moods.map((mood) => ({
            ...mood,
            group: item.group,
            audioName: item.name,
          }))
        );
    }
    return [];
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
          tmpGsvModel: item.gsvModel
            ? [item.gsvModel.group, item.gsvModel.name]
            : [],
          tmpModel: item.model ? [item.model.group, item.model.name] : [],
        };
      }
    );
    roleConfigs.value = data.roleConfigs.map((item: RoleModelConfig) => {
      return {
        ...item,
        tmpGsvModel: item.gsvModel
          ? [item.gsvModel.group, item.gsvModel.name]
          : [],
        tmpModel: item.model ? [item.model.group, item.model.name] : [],
      };
    });
    linesConfigs.value = data.linesConfigs.map((item: LinesConfig) => {
      return {
        ...item,
        tmpGsvModel: item.gsvModel
          ? [item.gsvModel.group, item.gsvModel.name]
          : [],
        tmpModel: item.model ? [item.model.group, item.model.name] : [],
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

  const gsvModelDataOptions = ref<CascaderOption[]>([]);

  const getGsvModels = async () => {
    const { data } = await gsvModels();
    gsvModelDataOptions.value = data
      .reduce((acc: any, item) => {
        const { group } = item;
        let groupItem = acc.find((g: GsvModel) => g.group === group);
        if (!groupItem) {
          groupItem = { group, list: [] } as any;
          acc.push(groupItem);
        }
        groupItem.list.push(item);
        return acc;
      }, [])
      .map((item) => {
        return {
          value: item.group,
          children: item.list.map((item1) => {
            return { value: item1.name };
          }),
        };
      });
  };

  const activeAudio = ref('');

  const playAudio = (data: any) => {
    const model = data.value;
    const group = data.parent.key;
    activeAudio.value = `${group}-${model}`;
    let url;
    speechModelData.value.forEach((item) => {
      item.speechModels.forEach((item1) => {
        if (item.group === group && item1.name === model) {
          url = item1.url;
        }
      });
    });
    if (audioElement.value) {
      // 设置音频源
      audioElement.value.src = url;
      // 播放音频
      audioElement.value.play();
    }
  };

  const playMoodAudio = (key: any, url) => {
    activeAudio.value = key;
    if (audioElement.value) {
      // 设置音频源
      audioElement.value.src = url;
      // 播放音频
      audioElement.value.play();
    }
  };

  const stopAudio = () => {
    if (audioElement.value) {
      // 停止音频播放
      audioElement.value.pause();
      audioElement.value.currentTime = 0; // 将播放进度设置为音频开头
    }
    activeAudio.value = '';
  };

  const handleAudioEnded = () => {
    activeAudio.value = '';
  };

  const handleLoadProjectRoleModel = async () => {
    const { data } = await loadProjectRoleModel({
      project: route.query.project as string,
      roles: roleConfigs.value.map((item) => item.role.role),
    });

    roleConfigs.value = roleConfigs.value.map((item: RoleModelConfig) => {
      const find = data.find((item1) => item1.role === item.role.role);
      if (find) {
        return {
          ...item,
          gsvModel: find.gsvModel,
          model: find.model,
          mood: find.mood,
          tmpGsvModel: find.gsvModel
            ? [find.gsvModel.group, find.gsvModel.name]
            : [],
          tmpModel: find.model ? [find.model.group, find.model.name] : [],
        } as any;
      }
      return {
        ...item,
        tmpGsvModel: item.gsvModel
          ? [item.gsvModel.group, item.gsvModel.name]
          : [],
        tmpModel: item.model ? [item.model.group, item.model.name] : [],
      };
    });
    linesConfigs.value = linesConfigs.value.map((item: LinesConfig) => {
      const find = data.find((item1) => item1.role === item.linesMapping.role);
      if (find) {
        return {
          ...item,
          gsvModel: find.gsvModel,
          model: find.model,
          mood: find.mood,
          tmpGsvModel: find.gsvModel
            ? [find.gsvModel.group, find.gsvModel.name]
            : [],
          tmpModel: find.model ? [find.model.group, find.model.name] : [],
        } as any;
      }
      return {
        ...item,
        tmpGsvModel: item.gsvModel
          ? [item.gsvModel.group, item.gsvModel.name]
          : [],
        tmpModel: item.model ? [item.model.group, item.model.name] : [],
      };
    });
  };

  const handleSetProjectRoleModel = async (config: RoleModelConfig) => {
    const { data } = await checkProjectRoleModel({
      project: route.query.project as string,
      role: config.role.role,
      gsvModel: config.gsvModel,
      model: config.model,
      mood: config.mood,
    } as any);
    if (!data) {
      Modal.warning({
        title: '已存在预置角色模型，是否覆盖配置？',
        content: () => false,
        async onOk() {
          const { msg } = await setProjectRoleModel({
            project: route.query.project as string,
            role: config.role.role,
            gsvModel: config.gsvModel,
            model: config.model,
            mood: config.mood,
          } as any);
          Message.success(msg);
        },
      });
    }
  };

  onMounted(() => {
    getGsvModels();
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
