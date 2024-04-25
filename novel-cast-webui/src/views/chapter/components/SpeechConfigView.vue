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
        <a-button
          type="primary"
          :disabled="!!processFlag"
          @click="handleStartSpeechesCreate"
          >开始生成语音</a-button
        >
        <a-space>
          <span>顺序播放开始序号</span>
          <a-input v-model="playStartIndex" style="width: 100px" />
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
        :columns="columns"
        :data="speechConfigs"
        :pagination="false"
        :size="'large'"
        :bordered="{ cell: true }"
        column-resizable
      >
        <template #model="{ record }">
          <a-cascader
            v-model="record.model"
            :options="speechModelData"
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
            :options="computedMoods(record.group + '-' + record.name)"
          ></a-select>
        </template>
        <template #operations="{ rowIndex, record }">
          <a-space size="medium">
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
          </a-space>
        </template>
      </a-table>
    </div>
    <div style="margin-bottom: 60px">
      <a-space style="margin-top: 20px; float: right" size="large">
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
    querySpeechConfigs,
    SpeechConfig,
    SpeechCreate,
    startSpeechesCreate,
  } from '@/api/chapter';
  import useLoading from '@/hooks/loading';
  import { querySpeechModels, SpeechModelGroup } from '@/api/model';
  import { CascaderOption, Message } from '@arco-design/web-vue';

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

  const emits = defineEmits(['closeDrawerFetchData', 'linesPointer']);
  const { loading, setLoading } = useLoading();

  const audioElement = ref<HTMLAudioElement | null>(null); // ref 对象引用到 audio 元素
  const activeAudioIndex = ref<number>(-1);
  const activeCreateIndex = ref<number>(-1);

  const processFlag = ref<boolean>(false);
  const speechConfigs = ref<SpeechConfig[]>([]);

  const columns = ref<TableColumnData[]>([
    {
      title: '序号',
      dataIndex: 'linesIndex',
      width: 80,
    },
    {
      title: '角色',
      dataIndex: 'role',
      width: 120,
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
      width: 80,
    },
    {
      title: '台词',
      dataIndex: 'lines',
    },
    {
      title: '操作',
      dataIndex: 'operations',
      slotName: 'operations',
      width: 150,
    },
  ]);

  const close = () => {
    emits('linesPointer', undefined);
    emits('closeDrawerFetchData');
  };

  const markedLines = (index: string) => {
    emits('linesPointer', index);
  };

  const getRoleSpeechData = async () => {
    const { data } = await querySpeechConfigs({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    });
    processFlag.value = data.processFlag;
    speechConfigs.value = data.speechConfigs.map((item) => {
      return {
        ...item,
        model: [item.group, item.name],
      };
    });
  };

  const playAll = ref(false);
  const playStartIndex = ref<string>('');

  const playAudio = (rowIndex: number, record: SpeechConfig) => {
    markedLines(record.linesIndex);
    if (audioElement.value) {
      // 设置音频源
      audioElement.value.src = record.audioUrl;
      // 播放音频
      audioElement.value.play();
      activeAudioIndex.value = rowIndex;
    }
  };

  const playNext = () => {
    activeAudioIndex.value += 1;
    const roleSpeech = speechConfigs.value[activeAudioIndex.value];
    if (roleSpeech) {
      setTimeout(() => {
        playAudio(activeAudioIndex.value, roleSpeech);
      }, 500);
    } else {
      activeAudioIndex.value = -1;
    }
  };

  const handleAudioEnded = () => {
    if (playAll.value && activeAudioIndex.value < speechConfigs.value.length) {
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
      speechConfigs.value.forEach((item, index) => {
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
    speechConfig: SpeechConfig
  ) => {
    try {
      setLoading(true);
      activeCreateIndex.value = rowIndex;
      await createSpeech({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
        speechConfig,
      } as SpeechCreate);
      await getRoleSpeechData();
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

  const handleStartSpeechesCreate = async () => {
    await startSpeechesCreate({
      project: route.query.project as string,
      chapterName: props.chapterName as string,
    } as ChapterParams);
    processFlag.value = true;
  };

  const handleCombineAudio = async () => {
    try {
      setLoading(true);
      const { msg } = await combineAudio({
        project: route.query.project as string,
        chapterName: props.chapterName as string,
      } as ChapterParams);
      Message.success(msg);
    } finally {
      setLoading(false);
    }
  };
  const hasEmptyAudioUrl = () => {
    // 检查是否有空的 audioUrl
    return (
      !speechConfigs.value ||
      speechConfigs.value.length === 0 ||
      speechConfigs.value.some(
        (item) =>
          !item.audioUrl || item.audioUrl === '' || item.audioUrl == null
      )
    );
  };

  const hasAudioUrl = () => {
    return (
      speechConfigs.value &&
      speechConfigs.value.length !== 0 &&
      speechConfigs.value.some((item) => item.audioUrl)
    );
  };

  let timer: any = null;
  const startFetchData = () => {
    if (hasEmptyAudioUrl()) {
      if (!timer) {
        timer = setInterval(() => {
          if (hasEmptyAudioUrl()) {
            getRoleSpeechData();
          } else {
            clearInterval(timer);
            timer = null;
          }
        }, 5000);
      }
    } else if (timer) {
      clearInterval(timer);
      timer = null;
    }
  };

  onMounted(() => {
    getSpeechModels();
    if (hasEmptyAudioUrl()) {
      startFetchData();
    }
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
