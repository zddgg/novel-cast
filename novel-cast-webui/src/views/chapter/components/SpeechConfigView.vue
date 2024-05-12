<template>
  <div class="container">
    <div style="width: 100%">
      <div
        style="
          display: flex;
          justify-content: space-between;
          margin-bottom: 20px;
        "
      >
        <a-space size="large">
          <a-button
            type="primary"
            status="success"
            size="large"
            :loading="loading"
            @click="handleCreateSpeechConfig"
            >生成语音配置
          </a-button>
          <a-button
            type="primary"
            :disabled="
              !!processFlag || !roleSpeechConfigs || !roleSpeechConfigs.length
            "
            @click="handleStartSpeechesCreate"
            >开始生成语音</a-button
          >
        </a-space>
        <a-space size="large">
          <div>
            <span style="margin-right: 10px">顺序播放开始序号</span>
            <a-input v-model="playStartIndex" style="width: 100px" />
          </div>
          <a-button
            type="primary"
            status="success"
            size="large"
            :disabled="!hasAudioUrl()"
            @click="playAllAudio"
            >顺序播放</a-button
          >
        </a-space>
      </div>
      <a-table
        id="speechConfigTable"
        :columns="columns"
        :data="roleSpeechConfigs"
        :pagination="false"
        :size="'large'"
        :bordered="{ cell: true }"
        column-resizable
        @row-click="rowClick"
      >
        <template #model="{ record }">
          <a-cascader
            v-model="record.model"
            :options="speechModelData"
            popup-container="#speechConfigTable"
            path-mode
            @change="(value) => {
              record.group = (value as string[])[0];
              record.name = (value as string[])[1];
            }"
          />
        </template>
        <template #mood="{ record }">
          <a-select
            v-model="record.mood"
            popup-container="#speechConfigTable"
            :options="computedMoods(record.group + '-' + record.name)"
          ></a-select>
        </template>
        <template #speedControl="{ record }">
          <a-select
            v-model="record.speedControl"
            default-value="1"
            popup-container="#speechConfigTable"
            :options="speedControlOptions"
          />
        </template>
        <template #lines="{ record }">
          <a-textarea v-model="record.lines"></a-textarea>
        </template>
        <template #operations="{ rowIndex, record }">
          <a-space direction="vertical" size="medium">
            <div>
              <a-button
                v-if="activeAudioIndex === rowIndex"
                type="outline"
                status="danger"
                @click="stopAudio"
              >
                <template #icon>
                  <icon-mute-fill />
                </template>
                停止
              </a-button>
              <a-button
                v-else
                type="outline"
                :disabled="!record.audioUrl"
                @click="playAudio(rowIndex, record)"
              >
                <template #icon>
                  <icon-sound-fill />
                </template>
                播放
              </a-button>
            </div>
            <a-popconfirm
              content="重新生成会覆盖原文件！"
              type="error"
              @ok="handleCreateRoleSpeech(rowIndex, record)"
            >
              <a-button
                type="primary"
                :loading="loading && activeCreateIndex === record.linesIndex"
                :disabled="loading"
              >
                重新生成
              </a-button>
            </a-popconfirm>
            <a-button
              :disabled="processFlag"
              :status="record.combineIgnore ? 'warning' : 'normal'"
              @click="() => (record.combineIgnore = !record.combineIgnore)"
            >
              {{ record.combineIgnore ? '已选择忽略' : '合成时忽略' }}
            </a-button>
          </a-space>
        </template>
      </a-table>
    </div>
    <div style="margin-bottom: 60px">
      <a-space style="margin-top: 20px; float: right" size="large">
        <div style="display: flex; align-items: center">
          <span style="white-space: nowrap; margin-right: 10px"
            >语音合成间隔</span
          >
          <a-input-number
            v-model="audioMergeInterval"
            :default-value="0"
            :step="100"
            :min="0"
            mode="button"
            style="width: 150px"
            @blur="
              () => {
                audioMergeInterval = Math.floor(audioMergeInterval / 100) * 100;
              }
            "
          />
          <span style="white-space: nowrap; margin-left: 10px">毫秒</span>
        </div>
        <a-button
          type="primary"
          status="danger"
          size="large"
          :loading="loading"
          :disabled="hasEmptyAudioUrl() || processFlag"
          @click="handleCombineAudio"
          >合成语音
        </a-button>
        <a-button size="large" @click="close">关闭</a-button>
      </a-space>
    </div>
    <audio ref="audioElement" @ended="handleAudioEnded"></audio>
  </div>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import { TableColumnData } from '@arco-design/web-vue/es/table/interface';
  import {
    ChapterParams,
    combineAudio,
    createSpeech,
    querySpeechConfig,
    RoleSpeechConfig,
    SpeechCreateParams,
    startSpeechesCreate,
    SpeechConfigParams,
    createSpeechConfig,
  } from '@/api/chapter';
  import useLoading from '@/hooks/loading';
  import { querySpeechModels, SpeechModelGroup } from '@/api/model';
  import { CascaderOption, Message, Modal } from '@arco-design/web-vue';

  const route = useRoute();
  const props = defineProps({
    speechConfigViewVisible: {
      type: Boolean,
      default: false,
    },
    chapterName: {
      type: String,
    },
  });

  const emits = defineEmits([
    'closeDrawerFetchData',
    'linesPointer',
    'refreshChapterText',
  ]);
  const { loading, setLoading } = useLoading();

  const audioElement = ref<HTMLAudioElement | null>(null); // ref 对象引用到 audio 元素
  const activeAudioIndex = ref<number>(-1);
  const activeCreateIndex = ref<number>(-1);

  const roleSpeechConfigs = ref<RoleSpeechConfig[]>([]);
  const audioMergeInterval = ref<number>(0);
  const processFlag = ref<boolean>(false);
  const creatingIndex = ref<string>('');

  const columns = ref<TableColumnData[]>([
    {
      title: '序号',
      dataIndex: 'linesIndex',
      width: 80,
    },
    {
      title: '角色',
      dataIndex: 'role',
    },
    {
      title: '模型',
      dataIndex: 'model',
      slotName: 'model',
    },
    {
      title: '感情',
      dataIndex: 'mood',
      slotName: 'mood',
    },
    {
      title: '速度控制',
      dataIndex: 'speedControl',
      slotName: 'speedControl',
      width: 100,
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
    },
  ]);

  const speedControlOptions = [
    {
      label: '0.25',
      value: 0.25,
    },
    {
      label: '0.5',
      value: 0.5,
    },
    {
      label: '0.75',
      value: 0.75,
    },
    {
      label: '1(正常)',
      value: 1,
    },
    {
      label: '1.25',
      value: 1.25,
    },
    {
      label: '1.5',
      value: 1.5,
    },
    {
      label: '1.75',
      value: 1.75,
    },
    {
      label: '2',
      value: 2,
    },
  ];

  const close = () => {
    emits('linesPointer', undefined);
    emits('closeDrawerFetchData');
  };

  const rowClick = (record) => {
    emits('linesPointer', record.linesIndex);
  };

  const markedLines = (index: string) => {
    emits('linesPointer', index);
  };

  const queryRoleSpeechData = () => {
    return querySpeechConfig({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    });
  };

  const getRoleSpeechData = async () => {
    const { data } = await queryRoleSpeechData();
    roleSpeechConfigs.value = data.roleSpeechConfigs.map((item) => {
      return {
        ...item,
        model: [item.group, item.name],
      };
    });
    processFlag.value = data.processFlag || false;
    audioMergeInterval.value = data.audioMergeInterval;
    creatingIndex.value = data.creatingIndex;
  };

  const playAll = ref(false);
  const playStartIndex = ref<string>('');

  const playAudio = (rowIndex: number, record: RoleSpeechConfig) => {
    markedLines(record.linesIndex);
    if (audioElement.value) {
      // 设置音频源
      audioElement.value.src = record.audioUrl;
      // 设置播放速度
      audioElement.value.playbackRate = record.speedControl || 1;
      // 播放音频
      audioElement.value.play();
      activeAudioIndex.value = rowIndex;
    }
  };

  const playNext = () => {
    activeAudioIndex.value += 1;
    const roleSpeech = roleSpeechConfigs.value[activeAudioIndex.value];
    if (roleSpeech) {
      setTimeout(() => {
        playAudio(activeAudioIndex.value, roleSpeech);
      }, 500);
    } else {
      activeAudioIndex.value = -1;
    }
  };

  const handleAudioEnded = () => {
    if (
      playAll.value &&
      activeAudioIndex.value < roleSpeechConfigs.value.length
    ) {
      playNext();
    } else {
      activeAudioIndex.value = -1;
      playAll.value = false;
    }
  };

  const stopAudio = () => {
    if (audioElement.value) {
      // 停止音频播放
      audioElement.value.pause();
      audioElement.value.currentTime = 0; // 将播放进度设置为音频开头
      activeAudioIndex.value = -1;
      playAll.value = false;
    }
  };

  const playAllAudio = () => {
    playAll.value = true;
    if (playStartIndex.value) {
      let start = -1;
      roleSpeechConfigs.value.forEach((item, index) => {
        if (item.linesIndex === playStartIndex.value) {
          start = index - 1;
        }
      });
      activeAudioIndex.value = start;
    }
    playNext();
  };

  const handleCreateRoleSpeech = async (
    rowIndex: number,
    roleSpeechConfig: RoleSpeechConfig
  ) => {
    try {
      setLoading(true);
      activeCreateIndex.value = rowIndex;
      await createSpeech({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
        roleSpeechConfig,
      } as SpeechCreateParams);
      await getRoleSpeechData();
      emits('refreshChapterText', props.chapterName);
    } finally {
      setLoading(false);
      activeCreateIndex.value = -1;
    }
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

  const computedMoods = (groupValue: string) => {
    return speechMood.value
      .flatMap((item) =>
        item.speechModels.map((model) => ({
          ...model,
          group: item.group,
        }))
      )
      .filter((item) => groupValue === `${item.group}-${item.name}`)
      .flatMap((item) => item.moods)
      .map((mood) => mood.name);
  };

  const handleCombineAudio = async () => {
    try {
      setLoading(true);
      const { msg } = await combineAudio({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
        speechConfig: {
          roleSpeechConfigs: roleSpeechConfigs.value,
          audioMergeInterval: audioMergeInterval.value,
        },
      } as SpeechConfigParams);
      Message.success(msg);
    } finally {
      setLoading(false);
    }
  };
  const hasEmptyAudioUrl = () => {
    // 检查是否有空的 audioUrl
    return (
      !roleSpeechConfigs.value ||
      roleSpeechConfigs.value.length === 0 ||
      roleSpeechConfigs.value.some(
        (item) =>
          !item.audioUrl || item.audioUrl === '' || item.audioUrl == null
      )
    );
  };

  const hasAudioUrl = () => {
    return (
      roleSpeechConfigs.value &&
      roleSpeechConfigs.value.length !== 0 &&
      roleSpeechConfigs.value.some((item) => item.audioUrl)
    );
  };

  let timer: any = null;
  const startFetchData = () => {
    if (!timer) {
      timer = setInterval(() => {
        if (processFlag.value) {
          getRoleSpeechData();
        } else {
          clearInterval(timer);
          timer = null;
        }
      }, 5000);
    }
  };

  const handleStartSpeechesCreate = async () => {
    await startSpeechesCreate({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    } as ChapterParams);
    processFlag.value = true;
    startFetchData();
  };

  const handleCreateSpeechConfigWr = async () => {
    try {
      setLoading(true);
      const { msg } = await createSpeechConfig({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
      } as ChapterParams);
      await getRoleSpeechData();
      Message.success(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSpeechConfig = () => {
    if (roleSpeechConfigs.value && roleSpeechConfigs.value.length) {
      Modal.warning({
        title: '生成语音配置',
        content: '再次生成会覆盖现有数据',
        onOk() {
          handleCreateSpeechConfigWr();
        },
      });
    } else {
      handleCreateSpeechConfigWr();
    }
  };

  onMounted(() => {
    getSpeechModels();
  });

  onUnmounted(() => {
    if (timer) {
      clearInterval(timer);
    }
  });

  watch(
    () => props.chapterName,
    () => {
      if (props.chapterName) {
        getRoleSpeechData();
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
