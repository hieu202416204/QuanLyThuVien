package BackEnd.Histories;

import java.util.ArrayList;
import java.util.List;

public class UserHistory {
    private List<UserInUserHistory> listHistory = new ArrayList<>();
    public UserHistory(){}
    public UserHistory(List<UserInUserHistory> listHistory){
        this.listHistory = listHistory;
    }

    public void updateListHistory(UserInUserHistory user) {
        this.listHistory.add(user);
    }

    public List<UserInUserHistory> getListHistory() {
        return this.listHistory;
    }
}
