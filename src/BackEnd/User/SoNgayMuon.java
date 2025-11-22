package BackEnd.User;

import java.time.LocalDate;

public class SoNgayMuon {
    private LocalDate muon;
    private LocalDate tra;
    public SoNgayMuon(LocalDate muon, LocalDate tra){
        this.muon = muon;
        this.tra = tra;
    }
    public long soNgayMuon() {
        if (muon == null || tra == null) return -1;
        long tongNgay = tra.toEpochDay() - muon.toEpochDay();
        return tongNgay < 0 ? -1 : tongNgay;
    }

}
