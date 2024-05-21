import axios from 'axios';
import { Pagination, PaginationResp } from '@/types/global';
import { GsvModel, ModelItem } from '@/api/model';

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

export interface RoleSpeechConfig {
  linesIndex: string;
  role: string;
  gender: string;
  ageGroup: string;
  lines: string;
  gsvModelGroup: string;
  gsvModelName: string;
  group: string;
  name: string;
  mood: string;
  audioUrl: string;
  model: string[];
  gsvModel: string[];
  duration: number;
  speedControl: number;
  textLanguage: number;
  combineIgnore: boolean;
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
  roleSpeechConfigs: RoleSpeechConfig[];
  audioMergeInterval: number;
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

export interface LinesParse {
  linesList: Lines[];
  linesModifiers: string[];
}

export function queryLines(params: ChapterParams) {
  return axios.post<LinesParse>('/api/chapter/lines', params);
}

export function linesUpdate(params: ChapterParams) {
  return axios.post('/api/chapter/linesUpdate', params);
}

export function parseLines(
  params: ChapterParams & { linesModifiers: string[] }
) {
  return axios.post<Lines[]>('/api/chapter/parseLines', params);
}

export interface RoleParams {
  project: string;
  chapterName: string;
  roles?: Role[];
}

export function updateRoles(params: RoleParams) {
  return axios.post('/api/chapter/updateRoles', params);
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
  model: ModelItem;
  strategyType: string;
  mood: string;
  tmpModel: string[];
  gsvModel: GsvModel;
  tmpGsvModel: string[];
}

export interface LinesConfig {
  linesMapping: LinesMapping;
  model: ModelItem;
  mood: string;
  tmpModel: string[];
  backup: string;
  gsvModel: GsvModel;
  tmpGsvModel: string[];
}

export interface ModelConfig {
  project: string;
  chapterName: string;
  commonRoleConfigs: RoleModelConfig[];
  roleConfigs: RoleModelConfig[];
  linesConfigs: LinesConfig[];
  aiProcess: boolean;
  aiIgnore: boolean;
  hasSpeechConfig: boolean;
}

export interface ModelConfigParams {
  project: string;
  chapterName: string;
  modelConfig: ModelConfig;
}

export function queryModelConfig(params: RoleParams) {
  return axios.post<ModelConfig>('/api/chapter/queryModelConfig', params);
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

export interface AiResultFormatParams {
  project: string;
  chapterName: string;
  aiResultText: string;
}

export function aiResultFormat(params: AiResultFormatParams) {
  return axios.post<ModelConfig>('/api/chapter/aiResultFormat', params);
}

export function saveAiReInferenceResult(params: AiResultFormatParams) {
  return axios.post<ModelConfig>(
    '/api/chapter/saveAiReInferenceResult',
    params
  );
}

export function loadAiResult(params: RoleParams) {
  return axios.post('/api/chapter/loadAiResult', params);
}

export function createSpeechConfig(params: ChapterParams) {
  return axios.post('/api/chapter/createSpeechConfig', params);
}

export interface SpeechConfig {
  roleSpeechConfigs: RoleSpeechConfig[];
  audioMergeInterval: number;
  processFlag: boolean;
  creatingIndex: string;
}

export interface SpeechConfigParams extends ChapterParams {
  speechConfig: SpeechConfig;
}

export function querySpeechConfig(params: RoleParams) {
  return axios.post<SpeechConfig>('/api/chapter/querySpeechConfig', params);
}

export function createSpeechesConfig(params: ChapterParams) {
  return axios.post('/api/chapter/createSpeechesConfig', params);
}

export function startSpeechesCreate(params: ChapterParams) {
  return axios.post('/api/chapter/startSpeechesCreate', params);
}

export interface SpeechCreateParams extends ChapterParams {
  roleSpeechConfig: RoleSpeechConfig;
}

export function createSpeech(params: SpeechCreateParams) {
  return axios.post<string>('/api/chapter/createSpeech', params);
}

export function combineAudio(params: SpeechConfigParams) {
  return axios.post('/api/chapter/combineAudio', params);
}
