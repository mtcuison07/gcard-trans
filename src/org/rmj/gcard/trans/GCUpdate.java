/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gcard.trans;

import org.rmj.appdriver.GRider;
import org.rmj.gcard.trans.pojo.UnitGCardMD;
import org.rmj.integsys.pojo.UnitGCard;
import org.rmj.integsys.pojo.UnitGCardDetailOffline;

/**
 *
 * @author kalyptus
 */
public class GCUpdate {
   //returns a pojo UnitGCardMD
   public Object loadTransaction(String fsTransNox) {
        UnitGCardMD loUnit = new UnitGCardMD();
        
        //load master
        GCard loGCard = new GCard();
        loGCard.setGRider(poGRider);
        loGCard.setWithParent(true);
        loUnit.setMaster((UnitGCard) loGCard.openRecord(fsTransNox));        
        
        //load offline
        GCOffPoints loOffline = new GCOffPoints();
        loOffline.setGRider(poGRider);
        loOffline.setWithParent(true);
        
        loUnit.setOffline(loOffline.loadLedger(loUnit.getMaster().getCardNumber(), false));
        return loUnit;
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

    public void setWithParent(boolean fbWithParent) {
        this.pbWithParnt = fbWithParent;
    }


// add methods here
    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
        this.psUserIDxx = foGRider.getUserID();
        if(psBranchCD.isEmpty())
            psBranchCD = poGRider.getBranchCode();
    }
   
    private boolean pbWithParnt = false;
    private String psBranchCD = "";
    private String psUserIDxx = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private GRider poGRider = null;
}
