import axios from 'axios';

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
