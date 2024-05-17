<template>
  <div class="container">
    <a-row :gutter="20" align="stretch">
      <a-col :span="24">
        <a-card class="general-card" title="模型列表">
          <a-row justify="space-between">
            <a-col :span="24">
              <a-tabs :default-active-tab="1" type="rounded">
                <a-tab-pane
                  v-for="(item, index) in renderData"
                  :key="index"
                  :title="item.group"
                >
                  <a-space wrap size="large">
                    <a-card v-for="(item1, index1) in item.list" :key="index1">
                      <a-space direction="vertical">
                        <a-descriptions
                          size="large"
                          :title="item1.name"
                          :column="1"
                          bordered
                          style="min-width: 350px"
                        >
                          <a-descriptions-item label="ckpt">
                            {{ item1.gptWeights }}
                          </a-descriptions-item>
                          <a-descriptions-item label="pth">
                            {{ item1.sovitsWeights }}
                          </a-descriptions-item>
                        </a-descriptions>
                      </a-space>
                    </a-card>
                  </a-space>
                </a-tab-pane>
                <a-button>新增</a-button>
              </a-tabs>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, ref } from 'vue';
  import { GsvModel, gsvModels } from '@/api/model';

  const renderData = ref<
    {
      group: string;
      list: GsvModel[];
    }[]
  >([]);

  const getGsvModels = async () => {
    const { data } = await gsvModels();
    renderData.value = data.reduce((acc: any, item) => {
      const { group } = item;
      let groupItem = acc.find((g: GsvModel) => g.group === group);
      if (!groupItem) {
        groupItem = { group, list: [] };
        acc.push(groupItem);
      }
      groupItem.list.push(item);
      return acc;
    }, []);
  };

  onMounted(() => {
    getGsvModels();
  });
</script>

<style scoped lang="less">
  .container {
    padding: 20px;
  }
</style>
