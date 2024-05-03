<template>
  <div class="container" :class="appStore.theme === 'dark' ? 'theme-dark' : 'theme-light'">
    <a-card class="general-card" :body-style="{ padding: '0' }">
      <a-layout>
        <a-layout-sider
          id="chapterConfigArea"
          ref="chapterConfigArea"
          style="width: 60%"
        >
          <a-card :bordered="false" :body-style="{ padding: '0' }">
            <n-scrollbar style="height: calc(89vh - 2rem - 40px)">
              <div style="padding: 20px">
                <a-space size="large" direction="vertical" style="width: 100%">
                  <div
                    v-for="(item, index) in chapters"
                    :key="index"
                    class="card-hover"
                    style="
                      border: 1px solid rgb(229, 230, 235);
                      border-radius: 2px;
                    "
                  >
                    <a-card
                      :bordered="false"
                      hoverable
                      style="
                        border: 1px solid rgb(229, 230, 235);
                        border-radius: 2px;
                      "
                    >
                      <div
                        style="width: 100%; display: flex; align-items: center"
                      >
                        <div style="width: 60%">
                          <span style="font-size: 1.8rem">
                            {{
                              item.chapterName
                                ? item.chapterName.substring(
                                    item.chapterName.indexOf('-') + 1
                                  )
                                : ''
                            }}
                          </span>
                        </div>
                        <div style="margin-left: 1rem">
                          <a-space>
                            <a-button
                              type="primary"
                              @click="showChapterText(item.chapterName)"
                              >展示文本
                            </a-button>
                            <div v-if="!!item.outAudioUrl">
                              <a-button
                                type="outline"
                                @click="playAudio(item, index)"
                              >
                                <template #icon>
                                  <icon-sound-fill />
                                </template>
                                配音播放
                              </a-button>
                            </div>
                            <a-button @click="reloadState(index, item)"
                              >刷新状态</a-button
                            >
                          </a-space>
                        </div>
                      </div>
                      <div style="width: 100%; margin-top: 3rem">
                        <a-steps
                          :current="item.step"
                          label-placement="vertical"
                        >
                          <a-step>
                            台词解析
                            <a-button
                              size="mini"
                              type="outline"
                              :disabled="item.step + 1 < 1"
                              @click="handleLinesView(item.chapterName)"
                              >查看
                            </a-button>
                          </a-step>
                          <a-step>
                            角色台词分析
                            <a-button
                              size="mini"
                              type="outline"
                              :disabled="item.step + 1 < 2"
                              @click="handleRoleLinesView(item.chapterName)"
                              >查看
                            </a-button>
                          </a-step>
                          <a-step>
                            模型选择
                            <a-button
                              size="mini"
                              type="outline"
                              :disabled="item.step + 1 < 3"
                              @click="handleModelSelectedView(item.chapterName)"
                              >查看
                            </a-button>
                          </a-step>
                          <a-step>
                            角色配音
                            <a-button
                              size="mini"
                              type="outline"
                              :disabled="item.step + 1 < 4"
                              @click="handleRoleSpeechView(item.chapterName)"
                              >查看
                            </a-button>
                          </a-step>
                          <a-step> 语音合成</a-step>
                        </a-steps>
                      </div>
                      <div
                        v-show="currentPlayingIndex === index"
                        style="margin-top: 20px"
                      >
                        <audio
                          ref="audioRefs"
                          style="width: 100%"
                          controls
                          @play="pauseOthers(index)"
                          @timeupdate="timeupdateThrottle"
                        >
                          <source :src="item.outAudioUrl" />
                        </audio>
                      </div>
                    </a-card>
                  </div>
                </a-space>
              </div>
            </n-scrollbar>
            <div style="height: 3rem">
              <a-card :bordered="false">
                <div
                  style="
                    width: 100%;
                    display: flex;
                    justify-content: center;
                    align-content: center;
                  "
                >
                  <a-pagination
                    :total="pagination.total || 0"
                    :page-size="pagination.pageSize"
                    :default-page-size="100"
                    :page-size-options="[100, 200, 300, 400, 500]"
                    :size="'large'"
                    show-jumper
                    show-page-size
                    @change="onPageChange"
                    @page-size-change="pageSizeChange"
                  />
                </div>
              </a-card>
            </div>
          </a-card>
        </a-layout-sider>
        <a-layout-content>
          <n-scrollbar style="height: calc(89vh)">
            <div id="text-review-area" style="padding: 4rem">
              <p
                v-for="(item, index) in chapterInfo.lineInfos"
                :key="index"
                class="content"
              >
                <span
                  v-for="(item1, index1) in item.sentenceInfos"
                  :id="`sentence-${item.index}-${item1.index}`"
                  :key="index1"
                  :style="item.index === 0 ? { fontSize: '2.5rem' } : {}"
                  :class="
                    highlightedSpanIds.includes(
                      `sentence-${item.index}-${item1.index}`
                    ) && 'highlightedText'
                  "
                >
                  {{ item1.content }}
                </span>
              </p>
            </div>
          </n-scrollbar>
        </a-layout-content>
      </a-layout>
    </a-card>
    <div>
      <a-drawer
        v-if="linesViewVisible"
        v-model:visible="linesViewVisible"
        height="100%"
        popup-container="#chapterConfigArea"
        :mask-closable="false"
        placement="top"
        title="台词解析"
        :footer="false"
        @cancel="handleDrawerClose"
      >
        <div style="padding-top: 20px">
          <LinesView
            v-model:lines-view-visible="linesViewVisible"
            v-model:role-lines-view-visible="roleLinesViewVisible"
            v-model:chapter-name="getChapterName"
            @lines-pointer="linesPointer"
            @close-drawer-fetch-data="closeDrawerFetchData"
          />
        </div>
      </a-drawer>
    </div>
    <div>
      <a-drawer
        v-if="roleLinesViewVisible"
        v-model:visible="roleLinesViewVisible"
        height="100%"
        popup-container="#chapterConfigArea"
        :mask-closable="false"
        :placement="'top'"
        title="角色台词分析"
        :footer="false"
        @cancel="handleDrawerClose"
      >
        <div style="padding-top: 20px">
          <AiInference
            v-model:role-lines-view-visible="roleLinesViewVisible"
            v-model:model-selected-view-visible="modelSelectedViewVisible"
            v-model:chapter-name="getChapterName"
            @lines-pointer="linesPointer"
            @lines-pointer-for-role="linesPointerForRole"
            @close-drawer-fetch-data="closeDrawerFetchData"
          />
        </div>
      </a-drawer>
    </div>
    <div>
      <a-drawer
        v-if="modelSelectedViewVisible"
        v-model:visible="modelSelectedViewVisible"
        height="100%"
        popup-container="#chapterConfigArea"
        :mask-closable="false"
        :placement="'top'"
        title="模型选择"
        :footer="false"
        @cancel="handleDrawerClose"
      >
        <div style="padding-top: 20px">
          <LinesMappingView
            v-model:model-selected-view-visible="modelSelectedViewVisible"
            v-model:speech-config-view-visible="speechConfigViewVisible"
            v-model:chapter-name="getChapterName"
            @lines-pointer="linesPointer"
            @close-drawer-fetch-data="closeDrawerFetchData"
          />
        </div>
      </a-drawer>
    </div>
    <div>
      <a-drawer
        v-if="speechConfigViewVisible"
        v-model:visible="speechConfigViewVisible"
        height="100%"
        popup-container="#chapterConfigArea"
        :mask-closable="false"
        :placement="'top'"
        title="角色配音"
        :footer="false"
        @cancel="handleDrawerClose"
      >
        <div style="padding-top: 20px">
          <RoleSpeechView
            v-model:speech-config-view-visible="speechConfigViewVisible"
            v-model:chapter-name="getChapterName"
            @lines-pointer="linesPointer"
            @close-drawer-fetch-data="closeDrawerFetchData"
          />
        </div>
      </a-drawer>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { Pagination } from '@/types/global';
  import { computed, onMounted, reactive, ref } from 'vue';
  import useLoading from '@/hooks/loading';
  import {
    ChapterInfo,
    Chapter,
    ChapterParams,
    queryChapterPageList,
    queryDetail,
  } from '@/api/chapter';
  import { useRoute } from 'vue-router';
  import AiInference from '@/views/chapter/components/RoleLinesView.vue';
  import RoleSpeechView from '@/views/chapter/components/SpeechConfigView.vue';
  import { useAppStore } from '@/store';
  import LinesView from './components/LinesView.vue';
  import LinesMappingView from './components/ModelSelectedView.vue';

  const route = useRoute();
  const { setLoading } = useLoading(true);
  const appStore = useAppStore();

  const chapters = ref<Chapter[]>([]);
  const chapterInfo = ref<ChapterInfo>({} as ChapterInfo);
  const highlightedSpanIds = ref<string[]>([]);

  const audioRefs = ref<HTMLAudioElement[]>([]);

  const currentPlayingIndex = ref(-1); // 当前播放音频的索引

  const getChapterName = computed(() => {
    if (chapterInfo.value.title) {
      return `${chapterInfo.value.index}-${chapterInfo.value.title}`;
    }
    return undefined;
  });

  const basePagination: Pagination = {
    current: 1,
    pageSize: 100,
  };
  const pagination = reactive({
    ...basePagination,
  });

  interface LinesDuration {
    start: number;
    end: number;
    index: string;
  }

  const durationMap = ref<Map<string, LinesDuration[]>>();

  const fetchData = async (
    params: ChapterParams = {
      current: 1,
      pageSize: 100,
    } as ChapterParams
  ) => {
    setLoading(true);
    try {
      params.project = route.query.project as string;
      const { data } = await queryChapterPageList(params);
      chapters.value = data.records;
      pagination.current = params.current;
      pagination.total = data.total;

      const resultMap = new Map<string, LinesDuration[]>();
      chapters.value.forEach((item) => {
        const list = item.speechConfigs;
        list.sort((a, b) => {
          // 将字符串分割成数组
          const [firstA, secondA] = a.linesIndex.split('-');
          const [firstB, secondB] = b.linesIndex.split('-');

          // 比较第一个数字
          if (firstA !== firstB) {
            return Number(firstA) - Number(firstB);
          }

          // 如果第一个数字相同，比较第二个数字
          return Number(secondA) - Number(secondB);
        });

        const linesDurations: LinesDuration[] = [];
        let startTime = 0;
        list.forEach((item1) => {
          const linesDuration: LinesDuration = {
            start: startTime,
            end: startTime + item1.duration,
            index: item1.linesIndex,
          };
          linesDurations.push(linesDuration);
          startTime += item1.duration;
        });
        resultMap.set(item.chapterName, linesDurations);
      });

      durationMap.value = resultMap;
    } catch (err) {
      // you can report use errorHandler or other
    } finally {
      setLoading(false);
    }
  };

  const onPageChange = (current: number) => {
    fetchData({ ...pagination, current } as ChapterParams);
  };

  const pageSizeChange = (pageSize: number) => {
    pagination.pageSize = pageSize;
    fetchData({ ...pagination, pageSize } as ChapterParams);
  };

  const scrollToTop = (id: string) => {
    const targetElement = document.getElementById(id);
    if (targetElement) {
      targetElement.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const scrollTo = (index: string) => {
    const targetElement = document.getElementById(`sentence-${index}`);
    if (targetElement) {
      // 获取窗口的大小
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;

      // 获取目标元素的位置
      const domRect = targetElement.getBoundingClientRect();

      // 调整目标元素位置，考虑外层容器的 padding
      const containerPadding =
        4 * parseFloat(getComputedStyle(targetElement).fontSize); // 4rem
      const adjustedTop = domRect.top - containerPadding;
      const adjustedBottom = domRect.bottom + containerPadding;
      const adjustedLeft = domRect.left;
      const adjustedRight = domRect.right;

      // 判断目标元素是否完全在窗口内
      const isFullyInViewport =
        adjustedTop >= 0 &&
        adjustedLeft >= 0 &&
        adjustedBottom <= windowHeight &&
        adjustedRight <= windowWidth;
      if (isFullyInViewport) {
        return;
      }

      targetElement.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const linesPointerForRole = (indexArr: string[]) => {
    highlightedSpanIds.value = indexArr.map((item) => {
      return `sentence-${item}`;
    });

    // 自定义排序函数
    indexArr.sort((a, b) => {
      // 将字符串分割成数组
      const [firstA, secondA] = a.split('-');
      const [firstB, secondB] = b.split('-');

      // 比较第一个数字
      if (firstA !== firstB) {
        return Number(firstA) - Number(firstB);
      }

      // 如果第一个数字相同，比较第二个数字
      return Number(secondA) - Number(secondB);
    });
    scrollTo(indexArr[0]);
  };

  const linesPointer = (index: string) => {
    highlightedSpanIds.value = [`sentence-${index}`];
    scrollTo(index);
  };

  const showChapterText = async (chapterName: string) => {
    setLoading(true);
    try {
      const { data } = await queryDetail({
        project: route.query.project,
        chapterName,
      } as ChapterParams);
      chapterInfo.value = data;
      scrollToTop('text-review-area');
    } catch (err) {
      // you can report use errorHandler or other
    } finally {
      setLoading(false);
    }
  };

  const linesViewVisible = ref(false);
  const roleLinesViewVisible = ref(false);
  const modelSelectedViewVisible = ref(false);
  const speechConfigViewVisible = ref(false);

  const reloadAudio = (index: number) => {
    audioRefs.value.forEach((audio, i) => {
      if (i === index) {
        audio.load();
      }
    });
  };

  const reloadState = (index: number, item: Chapter) => {
    fetchData();
    showChapterText(item.chapterName);
    reloadAudio(index);
  };

  const closeDrawerFetchData = () => {
    linesViewVisible.value = false;
    roleLinesViewVisible.value = false;
    modelSelectedViewVisible.value = false;
    speechConfigViewVisible.value = false;
    fetchData();
    setTimeout(() => {
      if (chapterInfo.value.index) {
        reloadAudio(chapterInfo.value.index);
      }
    }, 500);
  };

  const handleLinesView = (chapterName: string) => {
    showChapterText(chapterName);
    closeDrawerFetchData();
    linesViewVisible.value = true;
  };
  const handleRoleLinesView = (chapterName: string) => {
    showChapterText(chapterName);
    closeDrawerFetchData();
    roleLinesViewVisible.value = true;
  };
  const handleModelSelectedView = (chapterName: string) => {
    showChapterText(chapterName);
    closeDrawerFetchData();
    modelSelectedViewVisible.value = true;
  };
  const handleRoleSpeechView = (chapterName: string) => {
    showChapterText(chapterName);
    closeDrawerFetchData();
    speechConfigViewVisible.value = true;
  };
  const handleDrawerClose = () => {
    highlightedSpanIds.value = [];
  };

  const pauseOthers = (index: number) => {
    audioRefs.value.forEach((audio, i) => {
      if (i !== index && !audio.paused) {
        audio.pause();
      }
    });
  };

  const playAudio = (chapter: Chapter, index: number) => {
    currentPlayingIndex.value = index;
    showChapterText(chapter.chapterName);
    pauseOthers(index);
  };

  const findDialogue = (currentAudioTime: number) => {
    if (durationMap.value) {
      const chapterName = `${chapterInfo.value.index}-${chapterInfo.value.title}`;
      const linesDurations = durationMap.value?.get(chapterName);
      if (linesDurations) {
        for (let i = 0; i < linesDurations.length; i += 1) {
          const dialogue = linesDurations[i];
          if (
            dialogue.start <= currentAudioTime &&
            currentAudioTime < dialogue.end
          ) {
            return dialogue.index;
          }
        }
      }
    }
    return '未找到匹配的台词';
  };

  // eslint-disable-next-line @typescript-eslint/ban-types
  const useThrottle = (fn: Function, wait: number) => {
    let canRun = true;
    return (...args: any[]) => {
      if (canRun) {
        fn(...args);
        canRun = false;
        setTimeout(() => {
          canRun = true;
        }, wait);
      }
    };
  };

  const timeupdateThrottle = useThrottle((e: Event) => {
    const duration = Math.ceil(
      (e.target as HTMLAudioElement).currentTime * 1000
    );
    if (duration) {
      const index = findDialogue(duration);
      linesPointer(index);
    }
  }, 500);

  onMounted(() => {
    fetchData();
    audioRefs.value = Array.from(document.getElementsByTagName('audio'));
  });
</script>

<style scoped>
  .container {
    padding: 20px;
  }

  .card-hover:hover {
    transform: scale(1.015);
    transition: 0.3s;
  }

  .content {
    font-size: 18px;
    line-height: 1.5;
    text-indent: 2em;
  }

  .highlightedText {
    background-color: yellow;
    color: #000000;
  }
</style>
