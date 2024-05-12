<template>
  <div class="container">
    <div style="display: flex">
      <div style="width: 60%">
        <n-scrollbar style="height: calc(89vh - 40px)">
          <div style="padding: 0 20px">
            <a-space direction="vertical" size="medium" style="width: 100%">
              <a-card
                class="general-card"
                :body-style="{ padding: '20px' }"
                title="系统模型配置"
              >
                <a-form
                  ref="globalConfigFormRef"
                  :model="globalConfig"
                  size="large"
                  :label-col-props="{ span: 6 }"
                  :wrapper-col-props="{ span: 18 }"
                >
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item
                        label="默认模型"
                        field="defaultModel"
                        required
                      >
                        <a-cascader
                          v-model="globalConfig.defaultModel.tmpModel"
                          path-mode
                          :options="speechModelData"
                          @change="(value) => {
                    globalConfig.defaultModel.model = {
                      group:(value as string[])[0],
                      name:(value as string[])[1]
                    }
                  }"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="标题模型">
                        <a-cascader
                          v-model="globalConfig.titleModel.tmpModels"
                          path-mode
                          multiple
                          allow-clear
                          :options="speechModelData"
                          @change="(value) => {
                    globalConfig.titleModel.models = (value as []).map((item) => {
                              return {
                                group: item[0],
                                name: item[1],
                              }
                            })
                  }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="策略">
                        <a-select
                          v-model="globalConfig.titleModel.strategyType"
                          default-value="随机"
                        >
                          <a-option>随机</a-option>
                          <a-option>顺序</a-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="旁白模型">
                        <a-cascader
                          v-model="globalConfig.asideModel.tmpModels"
                          path-mode
                          multiple
                          allow-clear
                          :options="speechModelData"
                          @change="(value) => {
                    globalConfig.asideModel.models = (value as []).map((item) => {
                              return {
                                group: item[0],
                                name: item[1],
                              }
                            })
                  }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="策略">
                        <a-select
                          v-model="globalConfig.asideModel.strategyType"
                          default-value="随机"
                        >
                          <a-option>随机</a-option>
                          <a-option>顺序</a-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="观众模型">
                        <a-cascader
                          v-model="globalConfig.viewersModel.tmpModels"
                          path-mode
                          multiple
                          allow-clear
                          :options="speechModelData"
                          @change="(value) => {
                    globalConfig.viewersModel.models = (value as []).map((item) => {
                              return {
                                group: item[0],
                                name: item[1],
                              }
                            })
                  }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="策略">
                        <a-select
                          v-model="globalConfig.viewersModel.strategyType"
                          default-value="随机"
                        >
                          <a-option>随机</a-option>
                          <a-option>顺序</a-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>
              <a-card
                class="general-card"
                :body-style="{ padding: '20px' }"
                title="角色模型配置"
              >
                <a-form
                  :model="roleConfigs"
                  size="large"
                  :label-col-props="{ span: 6 }"
                  :wrapper-col-props="{ span: 18 }"
                >
                  <a-row
                    v-for="(item, index) in roleConfigs"
                    :key="index"
                    :gutter="24"
                  >
                    <a-col :span="7">
                      <a-form-item label="角色">
                        <a-input v-model="item.role" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="7">
                      <a-form-item label="模型">
                        <a-cascader
                          v-model="item.tmpModels"
                          path-mode
                          multiple
                          allow-clear
                          :options="speechModelData"
                          @change="(value) => {
                    item.models = (value as []).map((item1) => {
                              return {
                                group: item1[0],
                                name: item1[1],
                              }
                            })
                  }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="7">
                      <a-form-item label="策略">
                        <a-select
                          v-model="item.strategyType"
                          default-value="随机"
                        >
                          <a-option>随机</a-option>
                          <a-option>顺序</a-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="3">
                      <a-form-item>
                        <a-button @click="() => roleConfigs.splice(index, 1)"
                          >删除角色
                        </a-button>
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item>
                        <a-button type="primary" @click="handleAddRole"
                          >添加角色
                        </a-button>
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>
              <a-card
                class="general-card"
                :body-style="{ padding: '20px' }"
                title="音频配置"
              >
                <a-form
                  :model="audioConfig"
                  size="large"
                  :label-col-props="{ span: 6 }"
                  :wrapper-col-props="{ span: 18 }"
                >
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item
                        label="音频合并间隔"
                        :help="'多条音频合并时中间加入间隔，增加间隔会导致首页播放合并音频时，文本高亮会有一点偏差， 其他无影响'"
                      >
                        <a-input-number
                          v-model="audioConfig.audioMergeInterval"
                          :default-value="0"
                          :step="100"
                          :min="0"
                          mode="button"
                          class="input-demo"
                          @blur="
                            () => {
                              audioConfig.audioMergeInterval =
                                Math.floor(
                                  audioConfig.audioMergeInterval / 100
                                ) * 100;
                            }
                          "
                        />
                        <span style="margin-left: 10px; white-space: nowrap"
                          >毫秒</span
                        >
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>
              <a-card
                class="general-card"
                :body-style="{ padding: '20px' }"
                title="文本处理配置"
              >
                <a-form
                  :model="{}"
                  size="large"
                  :label-col-props="{ span: 6 }"
                  :wrapper-col-props="{ span: 18 }"
                >
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item
                        label="章节名正则"
                        help="右边选不到可以手动填，匹配文本是一行内容，不包括换行符"
                      >
                        <a-input v-model="textConfig.chapterTitlePattern" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="选择章节名正则">
                        <a-select
                          v-model="textConfig.chapterTitlePattern"
                          :default-value="chapterTitlePatterns[0].value"
                          :options="chapterTitlePatterns"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24" style="margin-top: 20px">
                    <a-col :span="24">
                      <a-form-item
                        label="章节名正则测试"
                        :label-col-props="{ span: 3 }"
                        :wrapper-col-props="{ span: 21 }"
                      >
                        <a-textarea
                          v-model="chapterTitlePatternTestText"
                          :auto-size="{ minRows: 1, maxRows: 20 }"
                        />
                        <a-button
                          type="primary"
                          style="margin-left: 20px"
                          @click="handleChapterTitlePatternTest"
                          >验证</a-button
                        >
                        <a-button
                          style="margin-left: 20px"
                          @click="
                            () => {
                              chapterTitlePatternTestText = '';
                              chapterTitlePatternTestResult = [];
                            }
                          "
                          >重置测试</a-button
                        >
                        <a-button
                          type="primary"
                          status="danger"
                          style="margin-left: 20px"
                          :loading="loading"
                          @click="createTmpChapters"
                          >生成章节</a-button
                        >
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row>
                    <a-col :span="12">
                      <a-form-item
                        v-if="
                          chapterTitlePatternTestResult &&
                          chapterTitlePatternTestResult.length
                        "
                      >
                        <a-table
                          :data="chapterTitlePatternTestResult"
                          :columns="[{ title: '章节名', dataIndex: 'text' }]"
                          :pagination="false"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-divider />
                  <a-row :gutter="24" style="margin-top: 20px">
                    <a-col :span="12">
                      <a-form-item
                        label="角色台词修饰符"
                        help="右边选不到可以在下拉框中手动填，紧密相连的一组符号，可配置多种，多层次只匹配最外层"
                      >
                        <a-input v-model="computedLinesModifiers" readonly />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="选择修饰符">
                        <a-select
                          v-model="textConfig.linesModifiers"
                          multiple
                          allow-create
                          :options="modifiersList"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24" style="margin-top: 20px">
                    <a-col :span="24">
                      <a-form-item
                        label="角色台词测试文本"
                        :label-col-props="{ span: 3 }"
                        :wrapper-col-props="{ span: 21 }"
                      >
                        <a-textarea
                          v-model="modifiersTestText"
                          :auto-size="{ minRows: 1, maxRows: 20 }"
                        />
                        <a-button
                          type="primary"
                          style="margin-left: 20px"
                          @click="handleModifiersTest"
                          >验证</a-button
                        >
                        <a-button
                          style="margin-left: 20px"
                          @click="
                            () => {
                              modifiersTestText = '';
                              modifiersTestResult = [];
                            }
                          "
                          >重置测试</a-button
                        >
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row>
                    <a-col :span="12">
                      <a-form-item
                        v-if="modifiersTestResult && modifiersTestResult.length"
                      >
                        <a-table
                          :data="modifiersTestResult"
                          :columns="[{ title: '台词文本', dataIndex: 'text' }]"
                          :pagination="false"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row>
                    <a-col :span="12">
                      <a-form-item label="文本语言">
                        <a-select
                          v-model="textConfig.textLanguage"
                          :default-value="textLanguageOptions[0].value"
                          :options="textLanguageOptions"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>
              <a-card class="general-card" :body-style="{ padding: '20px' }">
                <template #actions>
                  <a-space size="large">
                    <a-button
                      type="primary"
                      :loading="loading"
                      @click="handleProjectConfig"
                      >保存配置
                    </a-button>
                    <a-button @click="() => router.back()">返回</a-button>
                  </a-space>
                </template>
              </a-card>
            </a-space>
          </div>
        </n-scrollbar>
      </div>
      <div style="margin-left: 20px; width: 40%">
        <n-scrollbar style="height: calc(89vh - 40px)">
          <a-card
            v-if="splitChaptersResult && splitChaptersResult.length"
            class="general-card"
            :body-style="{ padding: '20px' }"
          >
            <a-table
              :data="splitChaptersResult"
              :columns="[{ title: '章节名', dataIndex: 'text' }]"
              :pagination="false"
              style="width: 100%"
            />
          </a-card>
        </n-scrollbar>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { querySpeechModels } from '@/api/model';
  import {
    CascaderOption,
    FormInstance,
    Message,
    Modal,
  } from '@arco-design/web-vue';
  import {
    createProjectConfig,
    modifiersTest,
    ProjectAudioConfig,
    ProjectConfig,
    ProjectGlobalConfig,
    ProjectRoleConfig,
    ProjectTextConfig,
    queryProjectConfig,
    splitTmpChapters,
  } from '@/api/project';
  import { useRoute, useRouter } from 'vue-router';
  import useLoading from '@/hooks/loading';
  import { ModelItem } from '@/api/chapter';

  const route = useRoute();
  const router = useRouter();
  const { loading, setLoading } = useLoading();

  const globalConfigFormRef = ref<FormInstance>();

  const globalConfig = ref<ProjectGlobalConfig>({
    defaultModel: {
      model: {} as ModelItem,
      strategyType: '',
    },
    titleModel: {
      models: [],
      strategyType: '',
    },
    asideModel: {
      models: [],
      strategyType: '',
    },
    viewersModel: {
      models: [],
      strategyType: '',
    },
  });

  const roleConfigs = ref<ProjectRoleConfig[]>([]);

  const audioConfig = ref<ProjectAudioConfig>({} as ProjectAudioConfig);
  const projectConfig = ref<ProjectConfig>({} as ProjectConfig);

  const handleAddRole = () => {
    roleConfigs.value.push({} as ProjectRoleConfig);
  };

  const speechModelData = ref<CascaderOption[]>([]);
  const getSpeechModels = async () => {
    const { data } = await querySpeechModels();
    speechModelData.value = data.map((item) => {
      return {
        value: item.group,
        children: item.speechModels.map((item1) => {
          return { value: item1.name };
        }),
      };
    });
  };

  const chapterTitlePatterns = [
    {
      label: '第*[章节卷集部篇回]*',
      value: '^\\s*第.{1,9}[章节卷集部篇回].*',
    },
    {
      label: '占一行的纯数字章节名',
      value: '^\\d+$',
    },
  ];

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

  const textLanguageOptions = [
    {
      label: 'zh',
      value: 'zh',
    },
    {
      label: 'en',
      value: 'en',
    },
  ];

  const textConfig = ref<ProjectTextConfig>({
    linesModifiers: [modifiersList[0].value],
    chapterTitlePattern: chapterTitlePatterns[0].value,
    textLanguage: textLanguageOptions[0].value,
  });

  const computedLinesModifiers = computed(() => {
    return textConfig.value.linesModifiers.join(',');
  });

  const chapterTitlePatternTestText = ref('');
  const chapterTitlePatternTestResult = ref<
    {
      text: string;
    }[]
  >([]);
  const splitChaptersResult = ref<
    {
      text: string;
    }[]
  >([]);

  const modifiersTestText = ref('');
  const modifiersTestResult = ref<
    {
      text: string;
    }[]
  >([]);

  const handleModifiersTest = async () => {
    const { data } = await modifiersTest({
      testText: modifiersTestText.value,
      linesModifiers: textConfig.value.linesModifiers,
    });
    modifiersTestResult.value = data.map((item) => {
      return { text: item };
    });
  };

  const handleChapterTitlePatternTest = () => {
    if (
      chapterTitlePatternTestText.value &&
      textConfig.value.chapterTitlePattern
    ) {
      const chapterNames = chapterTitlePatternTestText.value
        .split('\n')
        .map((item) => {
          if (new RegExp(textConfig.value.chapterTitlePattern).test(item)) {
            return item;
          }
          return null;
        });

      // 过滤掉 null 值，只保留匹配的章节名
      chapterTitlePatternTestResult.value = chapterNames
        .filter((item) => item !== null)
        .map((item) => {
          return { text: item as string };
        });
    }
  };

  const handleSplitTmpChapters = async () => {
    try {
      setLoading(true);
      const { data } = await splitTmpChapters({
        project: route.query.project as string,
        chapterTitlePattern: textConfig.value.chapterTitlePattern,
      });
      splitChaptersResult.value = data.map((item) => {
        return { text: item };
      });
      projectConfig.value.chapterNum = data.length;
    } finally {
      setLoading(false);
    }
  };

  const createTmpChapters = async () => {
    if (!textConfig.value.chapterTitlePattern) {
      Modal.warning({
        title: '没有正则只会生成一个全文章节',
        content: () => null,
        onOk: () => handleSplitTmpChapters(),
      });
    } else {
      await handleSplitTmpChapters();
    }
  };

  const handleProjectConfig = async () => {
    if (
      !globalConfig.value ||
      !globalConfig.value.defaultModel ||
      !globalConfig.value.defaultModel.model ||
      !globalConfig.value.defaultModel.model.name
    ) {
      globalConfigFormRef.value?.setFields({
        defaultModel: {
          status: 'error',
          message: '需要选择一个默认模型才能继续',
        },
      });
      Modal.warning({
        title: '需要选择一个默认模型才能继续',
        content: '系统模型配置===>默认模型',
      });
      return;
    }

    if (!projectConfig.value.chapterNum) {
      Modal.warning({
        title: '需要生成章节才能继续',
        content: '文本处理配置===>生成章节',
      });
      return;
    }

    const params: ProjectConfig = {
      project: route.query.project as string,
      globalConfig: globalConfig.value,
      roleConfigs: roleConfigs.value,
      audioConfig: audioConfig.value,
      textConfig: textConfig.value,
    } as ProjectConfig;
    try {
      setLoading(true);
      const { msg } = await createProjectConfig(params);
      Message.success(msg);
    } finally {
      setLoading(false);
    }
  };

  const getProjectConfigData = async () => {
    const { data } = await queryProjectConfig({
      project: route.query.project as string,
    });
    projectConfig.value = data;
    if (data.globalConfig) {
      globalConfig.value = data.globalConfig;
      globalConfig.value.defaultModel.tmpModel = [
        data.globalConfig.defaultModel.model.group,
        data.globalConfig.defaultModel.model.name,
      ];
      globalConfig.value.titleModel.tmpModels =
        data.globalConfig.titleModel.models.map((item) => {
          return [item.group, item.name];
        });
      globalConfig.value.asideModel.tmpModels =
        data.globalConfig.asideModel.models.map((item) => {
          return [item.group, item.name];
        });
      globalConfig.value.viewersModel.tmpModels =
        data.globalConfig.viewersModel.models.map((item) => {
          return [item.group, item.name];
        });
    }
    if (data.roleConfigs) {
      roleConfigs.value = data.roleConfigs?.map((item) => {
        return {
          ...item,
          tmpModels: item.models?.map((item1) => {
            return [item1.group, item1.name];
          }),
        };
      });
    }
    audioConfig.value = data.audioConfig;
    if (data.audioConfig) {
      audioConfig.value = data.audioConfig;
    } else {
      audioConfig.value = { audioMergeInterval: 0 };
    }
    textConfig.value = data.textConfig;
  };

  onMounted(() => {
    getSpeechModels();
    getProjectConfigData();
  });
</script>

<style scoped>
  .container {
    padding: 20px;
  }
</style>
