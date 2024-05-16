import axios from 'axios';
import { Pagination } from '@/types/global';
import { ModelItem } from '@/api/chapter';
import { GsvModel } from '@/api/model';

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
  project?: string;
  role: string;
  gsvModel: GsvModel;
  model: ModelItem;
  tmpGsvModel?: string[];
  tmpModel: string[];
  strategyType: string;
  mood: string;
}

export interface ProjectModelConfig {
  gsvModel: GsvModel;
  model: ModelItem;
  strategyType: string;
  tmpGsvModel?: string[];
  tmpModel?: string[];
  mood: string;
}

export interface ProjectGlobalConfig {
  defaultModel: ProjectModelConfig;
  titleModel: ProjectModelConfig;
  asideModel: ProjectModelConfig;
  viewersModel: ProjectModelConfig;
}

export interface ProjectAudioConfig {
  audioMergeInterval: number;
}

export interface ProjectTextConfig {
  chapterTitlePattern: string;
  linesModifiers: string[];
  textLanguage: string;
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

export interface LoadProjectRoleModel {
  project: string;
  roles: string[];
}

export function loadProjectRoleModel(params: LoadProjectRoleModel) {
  return axios.post<ProjectRoleConfig[]>(
    '/api/project/loadProjectRoleModel',
    params
  );
}

export function checkProjectRoleModel(params: ProjectRoleConfig) {
  return axios.post<boolean>('/api/project/checkProjectRoleModel', params);
}

export function setProjectRoleModel(params: ProjectRoleConfig) {
  return axios.post('/api/project/setProjectRoleModel', params);
}
