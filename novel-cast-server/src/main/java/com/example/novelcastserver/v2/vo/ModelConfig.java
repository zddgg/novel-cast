package com.example.novelcastserver.v2.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {
    private List<Group> gsvModelGroups;
    private List<GsvModel> gsvModels;
    private List<Group> gsvTimbreGroups;
    private List<GsvTimbre> gsvTimbres;
}
