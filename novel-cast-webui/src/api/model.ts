import axios from 'axios';

export interface ModelItem {
  group: string;
  name: string;
}

export interface Mood {
  name: string;
  url: string;
  text: string;
}

export interface SpeechModel {
  name: string;
  moods: Mood[];
  gender: string;
  ageGroup: string;
  url: string;
}

export interface SpeechModelGroup {
  index: number;
  group: string;
  speechModels: SpeechModel[];
}

export interface SpeechModelMarked {
  group: string;
  name: string;
  gender: string;
  ageGroup: string;
}

export function querySpeechModels() {
  return axios.post<SpeechModelGroup[]>('/api/model/speechModels');
}

export function speechMarked(params: SpeechModelMarked) {
  return axios.post('/api/model/speechMarked', params);
}

export interface GsvModel {
  id?: number;
  name: string;
  group: string;
  gptWeights?: string;
  sovitsWeights?: string;
}

export function gsvModels() {
  return axios.post<GsvModel[]>('/api/model/gsvModels');
}
