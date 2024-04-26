<template>
  <div class="container">
    <a-card class="general-card" title="项目管理">
      <a-grid :cols="4" :col-gap="24" :row-gap="24">
        <a-grid-item>
          <a-card
            style="height: 100%; width: 100%"
            :body-style="{
              minHeight: '160px',
              height: '100%',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
            }"
            @click="handleClick"
          >
            <div style="text-align: center">
              <div>
                <icon-plus />
              </div>
              <div style="margin-top: 20px; font-size: 18px">
                点击创建新项目
              </div>
            </div>
          </a-card>
        </a-grid-item>
        <a-grid-item v-for="(item, index) in renderData" :key="index">
          <div class="card-hover">
            <a-card>
              <a-descriptions
                size="large"
                :column="2"
                :title="item.projectName"
                bordered
              >
                <a-descriptions-item label="章节数">{{
                  item.chapterNum
                }}</a-descriptions-item>
                <!--                <a-descriptions-item label="状态">-->
                <!--                  <a-tag color="#00b42a">{{ item.status }}</a-tag>-->
                <!--                </a-descriptions-item>-->
              </a-descriptions>
              <div style="margin-top: 20px; text-align: right">
                <a-space size="large">
                  <a-button
                    type="primary"
                    @click="
                      () => {
                        router.push({
                          name: 'ProjectConfig',
                          query: {
                            project: item.projectName,
                          },
                        });
                      }
                    "
                    >step1: 项目配置</a-button
                  >
                  <a-button type="primary" @click="handleToChapterConfig(item)"
                    >step2: 章节配置</a-button
                  >
                </a-space>
              </div>
            </a-card>
          </div>
        </a-grid-item>
      </a-grid>
    </a-card>
    <a-modal
      v-model:visible="visible"
      title="创建新项目"
      :footer="false"
      :width="600"
      @ok="handleOk"
      @cancel="handleCancel"
      @close="fetchData"
    >
      <ProjectCreate v-model:visible="visible" />
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import useLoading from '@/hooks/loading';
  import { useRouter } from 'vue-router';
  import {
    preCheckProjectConfig,
    Project,
    ProjectParams,
    queryProjectList,
  } from '@/api/project';
  import ProjectCreate from '@/views/project/components/ProjectCreate.vue';
  import { Message } from '@arco-design/web-vue';

  const router = useRouter();

  const { loading, setLoading } = useLoading(true);
  const renderData = ref<Project[]>([]);
  const visible = ref(false);

  const handleClick = () => {
    visible.value = true;
  };
  const handleOk = () => {
    visible.value = false;
  };
  const handleCancel = () => {
    visible.value = false;
  };

  const fetchData = async (
    params: ProjectParams = {
      current: 1,
      pageSize: 20,
    } as ProjectParams
  ) => {
    setLoading(true);
    try {
      const { data } = await queryProjectList(params);
      renderData.value = data;
    } catch (err) {
      // you can report use errorHandler or other
    } finally {
      setLoading(false);
    }
  };

  const handleToChapterConfig = async (item: Project) => {
    const { data } = await preCheckProjectConfig({ project: item.projectName });
    if (!data) {
      Message.error('请先完成 step1: 项目配置');
      return;
    }
    await router.push({
      name: 'Chapter',
      query: {
        project: item.projectName,
      },
    });
  };

  fetchData();
</script>

<style scoped lang="less">
  .container {
    padding: 20px;
  }

  .card-hover:hover {
    transform: scale(1.02);
    transition: 0.3s;
  }
</style>
