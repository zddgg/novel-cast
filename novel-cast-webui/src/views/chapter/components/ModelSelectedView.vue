<template>
  <div class="container">
    <div>
      <a-form
        ref="formRef"
        size="large"
        :model="{}"
        :label-col-props="{ span: 8 }"
        :wrapper-col-props="{ span: 16 }"
      >
        <a-space direction="vertical" size="large">
          <a-card title="角色配置" :body-style="{ padding: '0' }">
            <a-card v-for="(item, index) in roleConfigs" :key="index">
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
            </a-card>
          </a-card>
          <a-card title="台词配置" :body-style="{ padding: 0 }">
            <a-card
              v-for="(item, index) in linesConfigs"
              :key="index"
              @click="handlePromptClick(item.linesMapping.linesIndex)"
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
    </div>
    <div style="margin-bottom: 60px">
      <a-space style="margin-top: 20px; float: right" size="large">
        <a-button
          type="primary"
          status="danger"
          size="large"
          :loading="loading"
          @click="handleStartSpeechesCreate"
          >保存模型配置并生成语音配置
        </a-button>
        <a-button
          type="primary"
          size="large"
          :loading="loading"
          @click="handleConfigUpdate"
          >保存
        </a-button>
        <a-button size="large" @click="close">下一步</a-button>
        <a-button size="large" @click="close">关闭</a-button>
      </a-space>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, ref, watch } from 'vue';
  import {
    LinesConfig,
    ModelItem,
    RoleModelConfig,
    queryModelConfig,
    updateModelConfig,
    ChapterParams,
    createSpeechesConfig,
    ModelConfig,
  } from '@/api/chapter';
  import { querySpeechModels, SpeechModelGroup } from '@/api/model';
  import { useRoute } from 'vue-router';
  import { CascaderOption, Message } from '@arco-design/web-vue';
  import useLoading from '@/hooks/loading';

  const route = useRoute();
  const props = defineProps({
    modelSelectedViewVisible: {
      type: Boolean,
      default: false,
    },
    speechConfigViewVisible: {
      type: Boolean,
      default: false,
    },
    chapterName: {
      type: String,
    },
  });

  const emits = defineEmits([
    'update:modelSelectedViewVisible',
    'update:speechConfigViewVisible',
    'linesPointer',
    'closeDrawerFetchData',
  ]);

  const { loading, setLoading } = useLoading();

  const roleConfigs = ref<RoleModelConfig[]>([]);

  const linesConfigs = ref<LinesConfig[]>([]);

  const handlePromptClick = (linesIndex: string) => {
    emits('linesPointer', linesIndex);
  };

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

  const close = () => {
    emits('linesPointer', undefined);
    emits('closeDrawerFetchData');
  };

  const getModelConfigData = async () => {
    const { data } = await queryModelConfig({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    });
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

  const handleConfigUpdate = async () => {
    try {
      setLoading(true);
      const { msg } = await updateModelConfig({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
        modelConfig: {
          roleConfigs: roleConfigs.value,
          linesConfigs: linesConfigs.value,
        } as ModelConfig,
      });
      Message.success(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleStartSpeechesCreate = async () => {
    try {
      setLoading(true);

      await updateModelConfig({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
        modelConfig: {
          roleConfigs: roleConfigs.value,
          linesConfigs: linesConfigs.value,
        } as ModelConfig,
      });
      const { msg } = await createSpeechesConfig({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
      } as ChapterParams);
      Message.success(msg);

      emits('update:modelSelectedViewVisible', false);
      emits('update:speechConfigViewVisible', true);
    } finally {
      setLoading(false);
    }
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

<style scoped>
  .container {
    padding: 0 20px;
  }
</style>
