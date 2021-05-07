/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gcard.trans;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.rmj.appdriver.GRider;
import org.rmj.integsys.pojo.UnitGCHistory;
import org.rmj.integsys.pojo.UnitGCard;
import org.rmj.integsys.pojo.UnitGCardDetailOffline;

/**
 *
 * @author kalyptus
 */
public class GCInquiry {
    public UnitGCard loadTransaction(String fsTransNox) {
        GCard loGCard = new GCard();
        UnitGCard loUGCard = new UnitGCard();
        loGCard.setGRider(poGRider);
        loGCard.setBranch(psBranchCD);
        loGCard.setWithParent(true);
        loUGCard = (UnitGCard) loGCard.openRecord(fsTransNox);

        setErrMsg(loGCard.getErrMsg());
        setMessage(loGCard.getMessage());

        return loUGCard;
    }

   public ArrayList<UnitGCardDetailOffline> loadOffLineLedger(String cardnmbr){
      System.out.println("GCInquiry.loadOffLineLedger");
      GCOffPoints loOffline = new GCOffPoints();
      loOffline.setGRider(poGRider);

      this.offline = loOffline.loadLedger(cardnmbr, false); //new ArrayList<UnitGCardDetailOffline>();
      //offline.addAll(loOffline.loadLedger(cardnmbr, false));

      setErrMsg(loOffline.getErrMsg());
      setMessage(loOffline.getMessage());

      return offline;
   }

    public ArrayList<UnitGCHistory> loadHistoryLedger(String fsTransNox){
        GCHistory loHist = new GCHistory();
        loHist.setGCardNo(fsTransNox);
        loHist.setGRider(poGRider);

        this.history = new ArrayList<UnitGCHistory>();
        history.addAll(loHist.loadLedger());

        setErrMsg(loHist.getErrMsg());
        setMessage(loHist.getMessage());

        return history;
    }

    public String getMessage() {
        return psWarnMsg;
    }

    public void setMessage(String fsMessage) {
        this.psWarnMsg = fsMessage;
    }

    public String getErrMsg() {
        return psErrMsgx;
    }

    public void setErrMsg(String fsErrMsg) {
        this.psErrMsgx = fsErrMsg;
    }

    public void setBranch(String foBranchCD) {
        this.psBranchCD = foBranchCD;
    }

    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
        this.psUserIDxx = foGRider.getUserID();
        if(psBranchCD.isEmpty())
           psBranchCD = poGRider.getBranchCode();

    }

    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private GRider poGRider = null;
    private String psBranchCD = "";
    private String psUserIDxx = "";

    ArrayList <UnitGCardDetailOffline> offline;
    ArrayList <UnitGCHistory> history;
}
