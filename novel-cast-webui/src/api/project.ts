import axios from 'axios';
import { Pagination } from '@/types/global';
import { ModelItem } from '@/api/chapter';

export interface Project {
  id: number;
  projectId: string;
  projectName: string;
  processState: string;
  chapterNum: string;
  status: 'online' | 'offline';
  createTime: string;
}

export interface ProjectParams extends Project, Pagination {}

export function queryProjectList(params: ProjectParams) {
  return axios.post<Project[]>('/api/project/list', params);
}

export function createProject(params: FormData) {
  return axios.post('/api/project/createProject', params);
}

export interface ProjectRoleConfig {
  role: string;
  models: ModelItem[];
  tmpModels: string[][];
  strategyType: string;
}

export interface ProjectModelConfig {
  model: ModelItem;
  strategyType: string;
  tmpModel?: string[];
}

export interface ProjectModelsConfig {
  models: ModelItem[];
  strategyType: string;
  tmpModels?: string[][];
}

export interface ProjectGlobalConfig {
  defaultModel: ProjectModelConfig;
  titleModel: ProjectModelsConfig;
  asideModel: ProjectModelsConfig;
  viewersModel: ProjectModelsConfig;
}

export interface ProjectAudioConfig {
  audioMergeInterval: number;
}

export interface ProjectTextConfig {
  chapterTitlePattern: string;
  linesModifiers: string[];
}

export interface ProjectConfig {
  project: string;
  globalConfig: ProjectGlobalConfig;
  roleConfigs: ProjectRoleConfig[];
  audioConfig: ProjectAudioConfig;
  textConfig: ProjectTextConfig;
  chapterNum: number;
}

export function createProjectConfig(params: ProjectConfig) {
  return axios.post('/api/project/createProjectConfig', params);
}

export function queryProjectConfig(params: { project: string }) {
  return axios.post<ProjectConfig>('/api/project/queryProjectConfig', params);
}

export function preCheckProjectConfig(params: { project: string }) {
  return axios.post<boolean>('/api/project/preCheckProjectConfig', params);
}

export function modifiersTest(params: {
  testText: string;
  linesModifiers: string[];
}) {
  return axios.post<string[]>('/api/project/modifiersTest', params);
}

export function splitTmpChapters(params: {
  project: string;
  chapterTitlePattern: string;
}) {
  return axios.post<string[]>('/api/project/splitTmpChapters', params);
}

export function deleteProject(params: { project: string }) {
  return axios.post('/api/project/deleteProject', params);
}
