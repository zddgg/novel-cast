<template>
  <div class="container">
    <a-space direction="vertical" size="large" style="width: 1460px">
      <a-card
        class="general-card"
        :body-style="{ padding: '20px' }"
        title="系统模型配置"
      >
        <a-form ref="globalConfigFormRef" :model="globalConfig" size="large">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="默认模型" field="defaultModel" required>
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
        <a-form :model="roleConfigs" size="large">
          <a-row v-for="(item, index) in roleConfigs" :key="index" :gutter="24">
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
                <a-select v-model="item.strategyType" default-value="随机">
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
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { querySpeechModels } from '@/api/model';
  import { CascaderOption, FormInstance, Message } from '@arco-design/web-vue';
  import {
    createProjectConfig,
    ProjectGlobalConfig,
    ProjectRoleConfig,
    queryProjectConfig,
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
          message: '默认模型必填',
        },
      });
      return;
    }

    const params = {
      project: route.query.project as string,
      globalConfig: globalConfig.value,
      roleConfigs: roleConfigs.value,
    };
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
