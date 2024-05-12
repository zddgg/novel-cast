<template>
  <div class="container">
    <a-row :gutter="20" align="stretch">
      <a-col :span="24">
        <a-card class="general-card" title="模型列表">
          <a-row justify="space-between">
            <a-col :span="24">
              <a-tabs :default-active-tab="1" type="rounded">
                <a-tab-pane
                  v-for="(item, index) in speechModelData"
                  :key="index"
                  :title="item.group"
                >
                  <a-grid :cols="4" :col-gap="24" :row-gap="24">
                    <a-grid-item
                      v-for="(item1, index1) in item.speechModels"
                      :key="index1"
                    >
                      <a-card hoverable>
                        <div
                          style="display: flex; justify-content: space-between"
                        >
                          <div>
                            <span
                              style="font-size: 1.5rem; margin-right: 10px"
                              >{{ item1.name }}</span
                            >
                            <a-tag
                              v-if="item1.gender && item1.ageGroup"
                              size="small"
                              color="green"
                            >
                              <template #icon>
                                <icon-check-circle-fill />
                              </template>
                              <span>已标记</span>
                            </a-tag>
                            <a-tag v-else size="small" color="red">
                              <template #icon>
                                <icon-close-circle-fill />
                              </template>
                              <span>未标记</span>
                            </a-tag>
                          </div>
                          <a-button
                            size="small"
                            type="primary"
                            @click="handleSpeechMarked(item.group, item1.name)"
                            >标记</a-button
                          >
                        </div>
                        <div
                          style="
                            margin-top: 20px;
                            display: flex;
                            justify-content: space-between;
                          "
                        >
                          <a-space>
                            <a-tag v-if="item1.gender" color="#165dff">{{
                              item1.gender
                            }}</a-tag>
                            <a-tag v-if="item1.ageGroup" color="#165dff">{{
                              item1.ageGroup
                            }}</a-tag>
                          </a-space>
                        </div>
                        <div style="margin-top: 20px">
                          <a-space wrap>
                            <div
                              v-for="(item2, index2) in item1.moods"
                              :key="index2"
                            >
                              <a-button
                                v-if="
                                  actionMood.key1 ===
                                  `${item.group}-${item1.name}-${item2.name}`
                                "
                                type="outline"
                                status="danger"
                                @click="stopAudio"
                              >
                                <template #icon>
                                  <icon-mute-fill />
                                </template>
                                {{ '停止' }}
                              </a-button>
                              <a-button
                                v-else
                                type="outline"
                                @click="
                                  playAudio(
                                    item2,
                                    `${item.group}-${item1.name}`
                                  )
                                "
                              >
                                <template #icon>
                                  <icon-sound-fill />
                                </template>
                                {{ item2.name }}
                              </a-button>
                            </div>
                          </a-space>
                        </div>
                        <div style="margin-top: 20px">
                          <a-typography-text
                            v-if="
                              actionMood.key === `${item.group}-${item1.name}`
                            "
                          >
                            {{ actionMood.text }}
                          </a-typography-text>
                        </div>
                      </a-card>
                    </a-grid-item>
                  </a-grid>
                </a-tab-pane>
                <a-button>新增</a-button>
              </a-tabs>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
    <audio ref="audioElement" @ended="handleAudioEnded"></audio>
    <a-modal
      v-model:visible="speechMarkedModalVisible"
      title="音频模型标记"
      @cancel="handleCancel"
      @before-ok="handleSpeechMarkedBeforeOk"
    >
      <a-form :model="speechMarkedForm">
        <a-form-item field="gender" label="gender">
          <a-select v-model="speechMarkedForm.gender">
            <a-option>男</a-option>
            <a-option>女</a-option>
            <a-option>未知</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="ageGroup" label="ageGroup">
          <a-select v-model="speechMarkedForm.ageGroup">
            <a-option>孩童</a-option>
            <a-option>青少年</a-option>
            <a-option>青年</a-option>
            <a-option>中年</a-option>
            <a-option>老年</a-option>
            <a-option>未知</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import {
    Mood,
    querySpeechModels,
    speechMarked,
    SpeechModelGroup,
    SpeechModelMarked,
  } from '@/api/model';

  const audioElement = ref<HTMLAudioElement | null>(null); // ref 对象引用到 audio 元素

  const speechModelData = ref<SpeechModelGroup[]>([]);

  const actionMood = ref<{ key: string; key1: string; text: string }>({
    key: '',
    key1: '',
    text: '',
  });

  const getSpeechModels = async () => {
    const { data } = await querySpeechModels();
    speechModelData.value = data;
  };

  const playAudio = (mood: Mood, key: string) => {
    if (audioElement.value) {
      // 设置音频源
      audioElement.value.src = mood.url;
      // 播放音频
      audioElement.value.play();
      actionMood.value.key = key;
      actionMood.value.key1 = `${key}-${mood.name}`;
      actionMood.value.text = mood.text;
    }
  };

  const resetActiveMood = () => {
    actionMood.value.key = '';
    actionMood.value.key1 = '';
    actionMood.value.text = '';
  };

  const stopAudio = () => {
    if (audioElement.value) {
      // 停止音频播放
      audioElement.value.pause();
      audioElement.value.currentTime = 0; // 将播放进度设置为音频开头
      resetActiveMood();
    }
  };

  const handleAudioEnded = () => {
    resetActiveMood(); // 音频播放完后停止播放
  };

  const speechMarkedModalVisible = ref(false);

  const speechMarkedForm = reactive<SpeechModelMarked>({
    group: '',
    name: '',
    gender: '',
    ageGroup: '',
  });

  const resetForm = () => {
    speechMarkedForm.group = '';
    speechMarkedForm.name = '';
    speechMarkedForm.gender = '';
    speechMarkedForm.ageGroup = '';
  };

  const handleSpeechMarked = (group: string, name: string) => {
    speechMarkedForm.group = group;
    speechMarkedForm.name = name;
    speechMarkedModalVisible.value = true;
  };

  const handleCancel = () => {
    resetForm();
    speechMarkedModalVisible.value = false;
  };

  const handleSpeechMarkedBeforeOk = async (
    done: (closed: boolean) => void
  ) => {
    if (speechMarkedForm.gender || speechMarkedForm.ageGroup) {
      await speechMarked(speechMarkedForm);
      await getSpeechModels();
      resetForm();
      done(true);
    } else {
      done(false);
    }
  };

  onMounted(() => {
    getSpeechModels();
  });
</script>

<style scoped lang="less">
  .container {
    padding: 20px;
  }
</style>
