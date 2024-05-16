<template>
  <div class="container">
    <div style="display: flex">
      <div style="width: 75%">
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
                    <a-col :span="8">
                      <a-form-item label="默认模型" field="defaultGsvModel">
                        <a-cascader
                          v-model="globalConfig.defaultModel.tmpGsvModel"
                          path-mode
                          :options="gsvModelDataOptions"
                          @change="(value) => {
                            globalConfig.defaultModel.gsvModel = {
                              group:(value as string[])[0],
                              name:(value as string[])[1]
                            }
                          }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="默认音色" field="defaultModel">
                        <a-cascader
                          v-model="globalConfig.defaultModel.tmpModel"
                          path-mode
                          :options="speechModelOptions"
                          @change="(value) => {
                            globalConfig.defaultModel.model = {
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
                                  activeAudio !==
                                  data.parent.key + '-' + data.value
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
                      <a-form-item label="默认情感" field="defaultMood">
                        <a-select v-model="globalConfig.defaultModel.mood">
                          <a-option
                            v-for="(item, index) in computedMoods(
                              globalConfig.defaultModel.model?.group,
                              globalConfig.defaultModel.model?.name
                            )"
                            :key="index"
                            :value="item.name"
                          >
                            <div
                              style="
                                display: flex;
                                justify-content: space-between;
                                align-items: center;
                                width: 100%;
                              "
                            >
                              <span>{{ item.name }}</span>
                              <a-button
                                v-if="
                                  activeAudio !==
                                  `${item.group}-${item.audioName}-${item.name}`
                                "
                                type="outline"
                                size="mini"
                                style="margin-left: 10px"
                                @click.stop="
                                  playMoodAudio(
                                    `${item.group}-${item.audioName}-${item.name}`,
                                    item.url
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
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="标题模型">
                        <a-cascader
                          v-model="globalConfig.titleModel.tmpGsvModel"
                          path-mode
                          :options="gsvModelDataOptions"
                          @change="(value) => {
                            globalConfig.titleModel.gsvModel = {
                              group:(value as string[])[0],
                              name:(value as string[])[1]
                            }
                          }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="标题音色">
                        <a-cascader
                          v-model="globalConfig.titleModel.tmpModel"
                          path-mode
                          :options="speechModelOptions"
                          @change="(value) => {
                            globalConfig.titleModel.model = {
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
                                  activeAudio !==
                                  data.parent.key + '-' + data.value
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
                      <a-form-item label="标题情感">
                        <a-select v-model="globalConfig.titleModel.mood">
                          <a-option
                            v-for="(item, index) in computedMoods(
                              globalConfig.titleModel.model?.group,
                              globalConfig.titleModel.model?.name
                            )"
                            :key="index"
                          >
                            <div
                              style="
                                display: flex;
                                justify-content: space-between;
                                align-items: center;
                                width: 100%;
                              "
                            >
                              <span>{{ item.name }}</span>
                              <a-button
                                v-if="
                                  activeAudio !==
                                  `${item.group}-${item.audioName}-${item.name}`
                                "
                                type="outline"
                                size="mini"
                                style="margin-left: 10px"
                                @click.stop="
                                  playMoodAudio(
                                    `${item.group}-${item.audioName}-${item.name}`,
                                    item.url
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
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="旁白模型">
                        <a-cascader
                          v-model="globalConfig.asideModel.tmpGsvModel"
                          path-mode
                          :options="gsvModelDataOptions"
                          @change="(value) => {
                            globalConfig.asideModel.gsvModel = {
                              group:(value as string[])[0],
                              name:(value as string[])[1]
                            }
                          }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="旁白音色">
                        <a-cascader
                          v-model="globalConfig.asideModel.tmpModel"
                          path-mode
                          :options="speechModelOptions"
                          @change="(value) => {
                            globalConfig.asideModel.model = {
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
                                  activeAudio !==
                                  data.parent.key + '-' + data.value
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
                      <a-form-item label="旁白情感">
                        <a-select v-model="globalConfig.asideModel.mood">
                          <a-option
                            v-for="(item, index) in computedMoods(
                              globalConfig.asideModel.model?.group,
                              globalConfig.asideModel.model?.name
                            )"
                            :key="index"
                          >
                            <div
                              style="
                                display: flex;
                                justify-content: space-between;
                                align-items: center;
                                width: 100%;
                              "
                            >
                              <span>{{ item.name }}</span>
                              <a-button
                                v-if="
                                  activeAudio !==
                                  `${item.group}-${item.audioName}-${item.name}`
                                "
                                type="outline"
                                size="mini"
                                style="margin-left: 10px"
                                @click.stop="
                                  playMoodAudio(
                                    `${item.group}-${item.audioName}-${item.name}`,
                                    item.url
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
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="观众模型">
                        <a-cascader
                          v-model="globalConfig.viewersModel.tmpGsvModel"
                          path-mode
                          :options="gsvModelDataOptions"
                          @change="(value) => {
                            globalConfig.viewersModel.gsvModel = {
                              group:(value as string[])[0],
                              name:(value as string[])[1]
                            }
                          }"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="观众音色">
                        <a-cascader
                          v-model="globalConfig.viewersModel.tmpModel"
                          path-mode
                          :options="speechModelOptions"
                          @change="(value) => {
                            globalConfig.viewersModel.model = {
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
                                  activeAudio !==
                                  data.parent.key + '-' + data.value
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
                      <a-form-item label="观众情感">
                        <a-select v-model="globalConfig.viewersModel.mood">
                          <a-option
                            v-for="(item, index) in computedMoods(
                              globalConfig.viewersModel.model?.group,
                              globalConfig.viewersModel.model?.name
                            )"
                            :key="index"
                          >
                            <div
                              style="
                                display: flex;
                                justify-content: space-between;
                                align-items: center;
                                width: 100%;
                              "
                            >
                              <span>{{ item.name }}</span>
                              <a-button
                                v-if="
                                  activeAudio !==
                                  `${item.group}-${item.audioName}-${item.name}`
                                "
                                type="outline"
                                size="mini"
                                style="margin-left: 10px"
                                @click.stop="
                                  playMoodAudio(
                                    `${item.group}-${item.audioName}-${item.name}`,
                                    item.url
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
                </a-form>
              </a-card>
              <a-card
                class="general-card"
                :body-style="{ padding: '20px' }"
                title="预置角色模型配置"
              >
                <a-form
                  :model="roleConfigs"
                  size="large"
                  :label-col-props="{ span: 6 }"
                  :wrapper-col-props="{ span: 18 }"
                >
                  <a-row v-for="(item, index) in roleConfigs" :key="index">
                    <a-col :span="5">
                      <a-form-item label="角色">
                        <a-input v-model="item.role" />
                      </a-form-item>
                    </a-col>
                    <a-col :span="5">
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
                    <a-col :span="5">
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
                                  activeAudio !==
                                  data.parent.key + '-' + data.value
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
                    <a-col :span="5">
                      <a-form-item label="情感">
                        <a-select v-model="item.mood">
                          <a-option
                            v-for="(item1, index1) in computedMoods(
                              item.model?.group,
                              item.model?.name
                            )"
                            :key="index1"
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
                    <a-col :span="4">
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
                          >验证
                        </a-button>
                        <a-button
                          style="margin-left: 20px"
                          @click="
                            () => {
                              chapterTitlePatternTestText = '';
                              chapterTitlePatternTestResult = [];
                            }
                          "
                          >重置测试
                        </a-button>
                        <a-popconfirm
                          type="warning"
                          content="非首次生成会删除所有章节重建！"
                          @ok="createTmpChapters"
                        >
                          <a-button
                            type="primary"
                            status="danger"
                            style="margin-left: 20px"
                            :loading="loading"
                            >生成章节
                          </a-button>
                        </a-popconfirm>
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
                          >验证
                        </a-button>
                        <a-button
                          style="margin-left: 20px"
                          @click="
                            () => {
                              modifiersTestText = '';
                              modifiersTestResult = [];
                            }
                          "
                          >重置测试
                        </a-button>
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
      <div style="margin-left: 20px; width: 25%">
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
    <audio ref="audioElement" @ended="handleAudioEnded"></audio>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import {
    GsvModel,
    gsvModels,
    querySpeechModels,
    SpeechModelGroup,
  } from '@/api/model';
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
    ProjectModelConfig,
    ProjectRoleConfig,
    ProjectTextConfig,
    queryProjectConfig,
    splitTmpChapters,
  } from '@/api/project';
  import { useRoute, useRouter } from 'vue-router';
  import useLoading from '@/hooks/loading';

  const route = useRoute();
  const router = useRouter();
  const { loading, setLoading } = useLoading();

  const audioElement = ref<HTMLAudioElement | null>(null); // ref 对象引用到 audio 元素

  const globalConfigFormRef = ref<FormInstance>();

  const globalConfig = ref<any>({
    defaultGsvModel: {},
    defaultModel: {},
    defaultMood: {},
    titleModel: {},
    asideModel: {},
    viewersModel: {},
  });

  const roleConfigs = ref<ProjectRoleConfig[]>([]);

  const audioConfig = ref<ProjectAudioConfig>({} as ProjectAudioConfig);
  const projectConfig = ref<ProjectConfig>({} as ProjectConfig);

  const handleAddRole = () => {
    roleConfigs.value.push({} as ProjectRoleConfig);
  };

  const speechModelData = ref<SpeechModelGroup[]>([]);
  const speechModelOptions = ref<CascaderOption[]>([]);
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
      label: '中文',
      value: 'all_zh',
    },
    {
      label: '英文',
      value: 'en',
    },
    {
      label: '日文',
      value: 'all_ja',
    },
    {
      label: '中英混合',
      value: 'zh',
    },
    {
      label: '日英混合',
      value: 'ja',
    },
    {
      label: '多语种混合',
      value: 'auto',
    },
  ];

  const textConfig = ref<ProjectTextConfig>({
    linesModifiers: [modifiersList[0].value],
    chapterTitlePattern: chapterTitlePatterns[0].value,
    textLanguage: textLanguageOptions[0].value,
  });

  const computedLinesModifiers = computed(() => {
    return textConfig.value?.linesModifiers.join(',');
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
      !globalConfig.value.defaultModel.gsvModel ||
      !globalConfig.value.defaultModel.gsvModel.name ||
      !globalConfig.value.defaultModel.model ||
      !globalConfig.value.defaultModel.model.name ||
      !globalConfig.value.defaultModel.mood
    ) {
      if (
        !globalConfig.value.defaultModel.gsvModel ||
        !globalConfig.value.defaultModel.gsvModel.name
      ) {
        globalConfigFormRef.value?.setFields({
          defaultGsvModel: {
            status: 'error',
            message: '需要选择一个默认模型才能继续',
          },
        });
      }
      if (
        !globalConfig.value.defaultModel.model ||
        !globalConfig.value.defaultModel.model.name
      ) {
        globalConfigFormRef.value?.setFields({
          defaultModel: {
            status: 'error',
            message: '需要选择一个默认音色才能继续',
          },
        });
      }
      if (!globalConfig.value.defaultModel.mood) {
        globalConfigFormRef.value?.setFields({
          defaultMood: {
            status: 'error',
            message: '需要选择一个默认情感才能继续',
          },
        });
      }

      Modal.warning({
        title: '需要选择一个默认配置才能继续',
        content: '系统模型配置===>默认模型、默认音色、默认情感',
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

  const toModelArr = (config: ProjectModelConfig) => {
    return {
      ...config,
      tmpGsvModel: config.gsvModel
        ? [config.gsvModel?.group, config.gsvModel?.name]
        : [],
      tmpModel: config.model ? [config.model?.group, config.model?.name] : [],
    };
  };

  const getProjectConfigData = async () => {
    const { data } = await queryProjectConfig({
      project: route.query.project as string,
    });
    projectConfig.value = data;
    if (data.globalConfig) {
      globalConfig.value = data.globalConfig;
      globalConfig.value.defaultModel = toModelArr(
        globalConfig.value.defaultModel
      );
      globalConfig.value.titleModel = toModelArr(globalConfig.value.titleModel);
      globalConfig.value.asideModel = toModelArr(globalConfig.value.asideModel);
      globalConfig.value.viewersModel = toModelArr(
        globalConfig.value.viewersModel
      );
    }
    if (data.roleConfigs) {
      roleConfigs.value = data.roleConfigs?.map((item) => {
        return {
          ...item,
          ...toModelArr(item),
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
    if (!textConfig.value) {
      textConfig.value = {
        chapterTitlePattern: '',
        linesModifiers: [],
        textLanguage: 'zh',
      };
    }
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

  onMounted(() => {
    getGsvModels();
    getSpeechModels();
    getProjectConfigData();
  });
</script>

<style scoped>
  .container {
    padding: 20px;
  }
</style>
