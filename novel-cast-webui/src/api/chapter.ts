import axios from 'axios';
import { Pagination, PaginationResp } from '@/types/global';

export interface SentenceInfo {
  index: number;
  content: string;
  lines: boolean;
}

export interface LineInfo {
  index: number;
  sentenceInfos: SentenceInfo[];
}

export interface ChapterInfo {
  index: number;
  title: string;
  lineInfos: LineInfo[];
}

export interface Role {
  role: string;
  gender: string;
  ageGroup: string;
  backup: string;
}

export interface Lines {
  index: string;
  lines: string;
  delFlag: boolean;
}

export interface LinesMapping {
  linesIndex: string;
  role: string;
  gender: string;
  ageGroup: string;
  mood: string;
  lines: string;
}

export interface SpeechConfig {
  linesIndex: string;
  role: string;
  lines: string;
  group: string;
  name: string;
  mood: string;
  audioUrl: string;
  model: string[];
  duration: number;
}
export interface AudioConfig {
  audioMergeInterval: number;
}

export interface Chapter {
  chapterName: string;
  content: string;
  markedText: string;
  linesList: Lines[];
  roles: Role[];
  linesMappings: LinesMapping[];
  step: number;
  outAudioUrl: string;
  speechConfigs: SpeechConfig[];
  audioConfig: AudioConfig;
}

export interface ChapterParams extends Chapter, Pagination {
  project: string;
  chapterName: string;
  linesList: Lines[];
}

export function queryChapterPageList(params: ChapterParams) {
  return axios.post<PaginationResp<Chapter>>('/api/chapter/pageList', params);
}

export function queryDetail(params: ChapterParams) {
  return axios.post<ChapterInfo>('/api/chapter/detail', params);
}

export function queryLines(params: ChapterParams) {
  return axios.post<Lines[]>('/api/chapter/lines', params);
}

export function linesUpdate(params: ChapterParams) {
  return axios.post<Lines[]>('/api/chapter/linesUpdate', params);
}

export function reCreateLines(params: ChapterParams) {
  return axios.post<Lines[]>('/api/chapter/reCreateLines', params);
}

export interface RoleParams {
  project: string;
  chapterName: string;
  roles?: Role[];
}

export function updateRoles(params: RoleParams) {
  return axios.post('/api/chapter/updateRoles', params);
}

export interface ModelItem {
  group: string;
  name: string;
}

export interface RoleModelConfig1 {
  role: Role;
  models: ModelItem[];
  strategyType: string;
  moods: string[];
  tmpModels: string[][];
}

export interface DataModel {
  model: string;
}

export interface SpeechModel {
  group: string;
  model: string;
  mood: string;
}

export interface RoleModelConfig {
  role: Role;
  models: ModelItem[];
  strategyType: string;
  moods: string[];
  tmpModels: string[][];
}

export interface LinesConfig {
  linesMapping: LinesMapping;
  model: ModelItem;
  mood: string;
  tmpModel: string[];
  backup: string;
}

export interface ModelConfig {
  project: string;
  chapterName: string;
  commonRoleConfigs: RoleModelConfig[];
  roleConfigs: RoleModelConfig[];
  linesConfigs: LinesConfig[];
  aiIgnore: boolean;
}

export interface ModelConfigParams {
  project: string;
  chapterName: string;
  modelConfig: ModelConfig;
}

export function queryModelConfig(params: RoleParams) {
  return axios.post('/api/chapter/queryModelConfig', params);
}

export function updateModelConfig(params: ModelConfigParams) {
  return axios.post('/api/chapter/updateModelConfig', params);
}

export interface AiResult {
  roles: Role[];
  linesMappings: LinesMapping[];
}

export function queryAiResult(params: RoleParams) {
  return axios.post<AiResult>('/api/chapter/aiResult', params);
}

export function ignoreAiResult(params: RoleParams) {
  return axios.post('/api/chapter/ignoreAiResult', params);
}

export function querySpeechConfigs(params: RoleParams) {
  return axios.post<{
    processFlag: boolean;
    speechConfigs: SpeechConfig[];
  }>('/api/chapter/querySpeechConfigs', params);
}

export function createSpeechesConfig(params: ChapterParams) {
  return axios.post('/api/chapter/createSpeechesConfig', params);
}

export function startSpeechesCreate(params: ChapterParams) {
  return axios.post('/api/chapter/startSpeechesCreate', params);
}

export interface SpeechCreate extends ChapterParams {
  speechConfig: SpeechConfig;
}

export function createSpeech(params: SpeechCreate) {
  return axios.post<string>('/api/chapter/createSpeech', params);
}

export function combineAudio(params: ChapterParams) {
  return axios.post('/api/chapter/combineAudio', params);
}
