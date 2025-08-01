package com.xuesinuo.pignoo.autodatabase.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DatabaseCheckResult {

    public static enum ResultState {
        SUCCESS, WARNING, ERROR
    }

    public ResultState state = ResultState.SUCCESS;

    public String advise2AddTable = "";
    public List<String> advise2AddColumn = new ArrayList<>();
    public List<String> advise2RemoveColumn = new ArrayList<>();
    public List<String> advise2UpdateColumn = new ArrayList<>();
    public List<String> otherMessage = new ArrayList<>();

    public void setState(ResultState state) {
        if (this.state.ordinal() < state.ordinal()) {
            this.state = state;
        }
    }
}
