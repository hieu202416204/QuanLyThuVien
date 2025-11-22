package BackEnd.Histories;

import BackEnd.Book.DayMT;

import java.util.ArrayList;
import java.util.List;

public class BookHistory {
    private List<DayMT> lichSuMuonSach = new ArrayList<>();
    public BookHistory(){
    }
    public BookHistory(List<DayMT> day){
        this.lichSuMuonSach = day;
    }
    public void setLichSuMuonSach(DayMT day){
        lichSuMuonSach.add(day);
    }
    public List<DayMT> getLichSuMuonSach(){
        return this.lichSuMuonSach;
    }
    public void inTTin(){
        for(DayMT dayMT : this.lichSuMuonSach){
            System.out.println("Ngày mượn: " + dayMT.getNgayMuon());
            System.out.println("Ngày trả: " + dayMT.getNgayTra());
        }
    }
}
