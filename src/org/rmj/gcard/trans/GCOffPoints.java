/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gcard.trans;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GTransaction;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.gcard.service.GCRestAPI;
import org.rmj.integsys.pojo.UnitGCard;
import org.rmj.integsys.pojo.UnitGCardDetailOffline;

/**
 *
 * @author kalyptus
 * Jheff [ 05/29/2010 03:35 pm ]
 *    - start coding this object.
 * Kalyptus [06/04/2010 11:16 am]
 *    - completed the code based on the template...
 */
public class GCOffPoints implements GTransaction {
    public Object newTransaction() {
        UnitGCardDetailOffline loOcc = new UnitGCardDetailOffline();

        Connection loCon = null;
        if(pbWithParnt)
           loCon = poGRider.getConnection();
        else
           loCon = poGRider.doConnect();

        if(psBranchCD.equals("")) {
           psBranchCD = poGRider.getBranchCode();
        }

        loOcc.setTransNo(MiscUtil.getNextCode(loOcc.getTable(), "sTransNox", true, loCon, psBranchCD));

        return loOcc;
    }

    public Object loadTransaction(String fsTransNox) {
        UnitGCardDetailOffline loOcc = new UnitGCardDetailOffline();
        Connection loCon = null;

       //If with parent then get the connection from parent else create a new connection
        if(pbWithParnt)
            loCon = poGRider.getConnection();
        else
            loCon = poGRider.doConnect();

        //retrieve the record
        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append(getSQ_Master());
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        Statement loStmt = null;
        ResultSet loRS = null;
        try {
            loStmt = loCon.createStatement();
            loRS = loStmt.executeQuery(lsSQL.toString());

            if(!loRS.next())
                setMessage("No Record Found!");
            else{
                //load each column to the entity
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loOcc.setValue(lnCol, loRS.getObject(lnCol));
                }
           }
        } catch (SQLException ex) {
            Logger.getLogger(GCApplication.class.getName()).log(Level.SEVERE, null, ex);
            setErrMsg(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
            if(!pbWithParnt)
               MiscUtil.close(loCon);
        }
        
        //mac 2020.08.25
        //  load offline transaction from main server
        UnitGCardDetailOffline loOff = loadTransServer(fsTransNox);
        if (loOff != null){
            if (!loOff.getTransNo().isEmpty()){
                if (Integer.parseInt(loOff.getTranStat()) > Integer.parseInt(loOcc.getTranStat())) return loOff;
            }
        }

        return loOcc;
    }

    //make sure that chip will be updated in this area
    public Object saveUpdate(Object foEntity, String fsTransNox) {
        String lsSQL = "";
        UnitGCardDetailOffline loOldEnt = null;
        UnitGCardDetailOffline loNewEnt = null;
        UnitGCardDetailOffline loResult = null;

        // Check for the value of foEntity
        if (!(foEntity instanceof UnitGCardDetailOffline)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return loResult;
        }

        // Typecast the Entity to this object
        loNewEnt = (UnitGCardDetailOffline) foEntity;

        if(loNewEnt.getGCardNo() == null || loNewEnt.getGCardNo().isEmpty()){
            setErrMsg("No Card detected!");
            return loResult;
        }

        if(loNewEnt.getTranDate() == null){
            setErrMsg("Invalid transaction date detected!");
            return loResult;
        }

        if(loNewEnt.getPoints() == null || loNewEnt.getPoints() == 0) {
            setErrMsg("Invalid transaction points detected! This record maybe encoded previously on ONLINE/OFFLINE entry.");
            return loResult;
        }

        if(loNewEnt.getSourceCd() == null || loNewEnt.getSourceCd().isEmpty()) {
            setErrMsg("Invalid transaction code detected!");
            return loResult;
        }

        if(loNewEnt.getSourceNo() == null || loNewEnt.getSourceNo().isEmpty()) {
            setErrMsg("Invalid Source Document Number Detected");
            return loResult;
        }

        //TODO: Test the user rights in these area...

        //Set the value of sModified and dModified here
        //2017.06.12 11:58AM
        //Remove encryption of set modification
        //GCrypt loCrypt = new GCrypt();
        //loNewEnt.setModifiedBy(loCrypt.encrypt(psUserIDxx));
        loNewEnt.setModifiedBy(psUserIDxx);
        loNewEnt.setDateModified(poGRider.getServerDate());

        //Generate the SQL Statement
        if (fsTransNox.equals("")) {
           Connection loCon = null;
           if(pbWithParnt)
              loCon = poGRider.getConnection();
           else
              loCon = poGRider.doConnect();

           loNewEnt.setValue(1, MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loCon, psBranchCD));

           //Generate the INSERT statement
            lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt);
        } else {
            //Reload previous record
            loOldEnt = (UnitGCardDetailOffline) loadTransaction(fsTransNox);
            //Generate the UPDATE statement

            lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt, (GEntity)loOldEnt,
                         "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        }

        //No changes has been made
        if (lsSQL.equals("")) {
            setMessage("Record is not updated!");
            return loResult;
        }

        if(!pbWithParnt)
           poGRider.beginTrans();

        System.out.println(lsSQL.toString());
        if(poGRider.executeQuery(lsSQL.toString(), loNewEnt.getTable(), "", "") == 0){
           if(!poGRider.getErrMsg().isEmpty())
              setErrMsg(poGRider.getErrMsg());
           else
              setMessage("No record updated");
        }
        else
           loResult = loNewEnt;

        if(!pbWithParnt){
           if(getErrMsg().isEmpty())
              poGRider.commitTrans();
           else
              poGRider.rollbackTrans();
        }

        return loResult;
    }

    public boolean deleteTransaction(String fsTransNox) {
        UnitGCardDetailOffline  loOcc = (UnitGCardDetailOffline) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
           setMessage("No record found!");
           return lbResult;
        }

        //TODO: Test the user rights in these area...

        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append("DELETE FROM " + loOcc.getTable());
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        if(!pbWithParnt)
           poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
           if(!poGRider.getErrMsg().isEmpty())
              setErrMsg(poGRider.getErrMsg());
           else
              setMessage("No record deleted");
        }
        else
           lbResult = true;

        if(!pbWithParnt){
           if(getErrMsg().isEmpty())
              poGRider.commitTrans();
           else
              poGRider.rollbackTrans();
        }

        return lbResult;
    }

    public boolean closeTransactionOnline(String fsTransNox){
        try {
            JSONObject loJSON = GCRestAPI.VerifyOfflineEntry(poGRider, fsTransNox);
            String lsResult =  (String) loJSON.get("result");
            
            if (lsResult.equalsIgnoreCase("success")){
                setMessage((String) loJSON.get("message"));
                return true;
            }
            
            loJSON = (JSONObject) new JSONParser().parse(loJSON.get("error").toString());
            setErrMsg((String) loJSON.get("message"));
            setMessage("Unable to verify transaction.");
        } catch (ParseException ex) {
            Logger.getLogger(GCOffPoints.class.getName()).log(Level.SEVERE, null, ex);
            setErrMsg(ex.getMessage());
            setMessage("Unable to verify transaction.");
        }
        
        return false;
    }
    
    //Used in verifying the offline transaction
    //fsTransnox = "sTransNox»sCompnyID»sSourceNo»sSourceCd
    public boolean closeTransaction(String fsTransNox) {
        UnitGCardDetailOffline  loOcc = (UnitGCardDetailOffline) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
           setMessage("No record found!");
           return lbResult;
        }

        //exit if transaction is not in OPEN status
        if(!loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_OPEN)){
           setMessage("Current record is not in OPEN status!");
           return lbResult;
        }

        //TODO: Test the user rights in these area...

        //GCrypt loCrypt = new GCrypt();

        StringBuilder lsSQL = new StringBuilder();

        lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
        lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED));
        lsSQL.append(", sPostedxx = " + SQLUtil.toSQL(psUserIDxx));
        lsSQL.append(", dPostedxx = " + SQLUtil.toSQL(poGRider.getServerDate()));
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        if(!pbWithParnt)
           poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
           if(!poGRider.getErrMsg().isEmpty())
              setErrMsg(poGRider.getErrMsg());
           else
              setMessage("No record updated");
        }
        else
           lbResult = true;

        if(lbResult){
            if(!updateMaster(loOcc.getGCardNo(), loOcc.getPoints())){
                if(!pbWithParnt) 
                  poGRider.rollbackTrans();

                setMessage("Unable to update GCard Master table");
                return false;
            }
        }

        if(!pbWithParnt){
           if(getErrMsg().isEmpty())
              poGRider.commitTrans();
           else
              poGRider.rollbackTrans();
        }

        return lbResult;
    }
    
    //update Master Record
    //Update the Total Points Only/Available point are updated in the GCUpdate 
    private boolean updateMaster(String cardno, Double points){
        String lsSQL;
        boolean lbOk = false;
        System.out.println("updateMaster"); 
        GCard loGCard = new GCard();
        loGCard.setGRider(poGRider);
        loGCard.setWithParent(true);
        loGCard.setBranch(psBranchCD);
        UnitGCard loUGCard = (UnitGCard) loGCard.openRecord(cardno);

        lsSQL = "UPDATE G_Card_Master" + 
                " SET nTotPoint = nTotPoint + " + points.toString() + 
                " WHERE sGCardNox = " + SQLUtil.toSQL(loUGCard.getGCardNo());

        if(poGRider.executeQuery(lsSQL, "G_Card_Master", "", "") == 0){
            if(!poGRider.getErrMsg().isEmpty())
               setErrMsg(poGRider.getErrMsg());
            else
               setMessage("No record updated");
        }else
            lbOk = true; 

        return lbOk;
    }

    public boolean postTransaction(String fsTransNox) {
        UnitGCardDetailOffline  loOcc = (UnitGCardDetailOffline) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
           setMessage("No record found!");
           return lbResult;
        }

        //exit if transaction is not in CLOSED status
        if(!loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED)){
           setMessage("Current record is not in CLOSED status!");
           return lbResult;
        }

        //TODO: Test the user rights in these area...

        //GCrypt loCrypt = new GCrypt();
        StringBuilder lsSQL = new StringBuilder();

        lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
        lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_POSTED));
        lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
        lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        if(!pbWithParnt)
           poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
           if(!poGRider.getErrMsg().isEmpty())
              setErrMsg(poGRider.getErrMsg());
           else
              setMessage("No record updated");
        }
        else
           lbResult = true;

        if(lbResult){
            //TODO: Write the information to the GCard here
         }

        if(!pbWithParnt){
           if(getErrMsg().isEmpty()){
              poGRider.commitTrans();
           }
           else
              poGRider.rollbackTrans();
        }

        return lbResult;
    }

    public boolean voidTransaction(String fsTransNox) {
        UnitGCardDetailOffline  loOcc = (UnitGCardDetailOffline) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
           setMessage("No record found!");
           return lbResult;
        }

        //exit if transaction is in CANCELLED status
        if(loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED)){
           setMessage("Current record is CANCELLED!");
           return lbResult;
        }

        StringBuilder lsSQL = new StringBuilder();
        if(loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED)){
           setMessage("Current record is POSTED!");
           return lbResult;          
        }
        else{
            String []lasTrans = fsTransNox.split("»");
            lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
            lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED));
            lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
            lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
            lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));
        }
        if(!pbWithParnt)
           poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
                setMessage("No record updated");
        }
        else
            lbResult = true;

        if(!pbWithParnt){
            if(getErrMsg().isEmpty())
                poGRider.commitTrans();
            else
                poGRider.rollbackTrans();
        }

        return lbResult;
    }

    public boolean cancelTransaction(String fsTransNox) {
        UnitGCardDetailOffline  loOcc = (UnitGCardDetailOffline) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
            setMessage("No record found!");
            return lbResult;
        }

        //exit if transaction is not in CLOSED status
        if(loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED)){
            setMessage("Current record is in CANCELLED status!");
            return lbResult;
        }

        StringBuilder lsSQL = new StringBuilder();
        //GCrypt loCrypt = new GCrypt();
        if(loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED)){
           setMessage("Current record is in POSTED status!");
           return lbResult;
        }
        else{
            lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
            lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED));
            lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
            lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
            lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));
        }

        if(!pbWithParnt)
            poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
                setMessage("No record updated");
        }
        else
            lbResult = true;

        if(!pbWithParnt){
            if(getErrMsg().isEmpty())
               poGRider.commitTrans();
            else
               poGRider.rollbackTrans();
        }

        return lbResult;
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

    public String getSQ_Master() {
       return (MiscUtil.makeSelect(new UnitGCardDetailOffline()));
    }


 // add methods here
    public void setGRider(GRider foGRider) {
       this.poGRider = foGRider;
       this.psUserIDxx = foGRider.getUserID();
       if(psBranchCD.isEmpty())
          psBranchCD = poGRider.getBranchCode();
    }

    public Date getTranDate() {
       return poTransact;
    }

    public void setTranDate(Date date) {
       this.poTransact = date;
    }

    private UnitGCardDetailOffline loadTransServer(String fsTransNox){
        ArrayList<UnitGCardDetailOffline> loArray = new ArrayList<>();
        JSONObject oJson = GCRestAPI.RequestOffline(poGRider, fsTransNox);
        
        String result = (String) oJson.get("result");
        if(!result.equalsIgnoreCase("SUCCESS")){
            psErrMsgx = oJson.get("error").toString();
            return null;
        }

        JSONArray oJArray = (JSONArray) oJson.get("detail");
        Iterator<JSONObject> iterator = oJArray.iterator();
        
        if (iterator.hasNext()){
            UnitGCardDetailOffline oOff = new UnitGCardDetailOffline();
            JSONObject oJDetl = iterator.next();
            oOff.setTransNo((String) oJDetl.get("sTransNox") );
            oOff.setGCardNo((String) oJDetl.get("sGCardNox") );
            oOff.setCompanyID((String) oJDetl.get("sCompnyID") );
            if (oJDetl.get("dTransact") != null) oOff.setTranDate(SQLUtil.toDate((String) oJDetl.get("dTransact"), SQLUtil.FORMAT_SHORT_DATE));
            oOff.setSourceNo((String) oJDetl.get("sSourceNo") );
            oOff.setSourceCd((String) oJDetl.get("sSourceCd") );
            oOff.setTranAmount(Double.valueOf(oJDetl.get("nTranAmtx").toString()));
            oOff.setPoints(Double.valueOf(oJDetl.get("nPointsxx").toString()));
            oOff.setTranStat((String) oJDetl.get("cTranStat") );
            oOff.setPostedBy((String) oJDetl.get("sPostedxx") );
            if (oJDetl.get("dPostedxx") != null) oOff.setDatePosted(SQLUtil.toDate((String) oJDetl.get("dPostedxx"), SQLUtil.FORMAT_SHORT_DATE));
            oOff.setModifiedBy((String) oJDetl.get("sModified") );
            if (oJDetl.get("dModified") != null) oOff.setDateModified(SQLUtil.toDate((String) oJDetl.get("dModified"), SQLUtil.FORMAT_TIMESTAMP));
            
            return oOff;
        }
        
        psErrMsgx = "No record found on the main server.";
        return null;
    }
    
    // if isall is false load only close transaction to post points to the card
    // contrary to the previous GCard System. Ledger are extracted from the central server...
    public ArrayList<UnitGCardDetailOffline> loadLedger(String cardnmbr, boolean isall) {
        ArrayList<UnitGCardDetailOffline> loArray = new ArrayList<>();
        JSONObject oJson = GCRestAPI.RequestOfflineHistory(poGRider, cardnmbr, isall ? "all" : "1");
        
        String result = (String) oJson.get("result");
        if(!result.equalsIgnoreCase("SUCCESS")){
            psErrMsgx = oJson.get("error").toString();
            return null;
        }

        JSONArray oJArray = (JSONArray) oJson.get("detail");
        Iterator<JSONObject> iterator = oJArray.iterator();
        while (iterator.hasNext()) {
            UnitGCardDetailOffline oOff = new UnitGCardDetailOffline();
            JSONObject oJDetl = iterator.next();
            oOff.setTransNo((String) oJDetl.get("sTransNox") );
            oOff.setGCardNo((String) oJDetl.get("sGCardNox") );
            oOff.setCompanyID((String) oJDetl.get("sCompnyID") );
            if (oJDetl.get("dTransact") != null) oOff.setTranDate(SQLUtil.toDate((String) oJDetl.get("dTransact"), SQLUtil.FORMAT_SHORT_DATE));
            oOff.setSourceNo((String) oJDetl.get("sSourceNo") );
            oOff.setSourceCd((String) oJDetl.get("sSourceCd") );
            oOff.setTranAmount(Double.valueOf(oJDetl.get("nTranAmtx").toString()));
            oOff.setPoints(Double.valueOf(oJDetl.get("nPointsxx").toString()));
            oOff.setTranStat((String) oJDetl.get("cTranStat") );
            oOff.setPostedBy((String) oJDetl.get("sPostedxx") );
            if (oJDetl.get("dPostedxx") != null) oOff.setDatePosted(SQLUtil.toDate((String) oJDetl.get("dPostedxx"), SQLUtil.FORMAT_SHORT_DATE));
            oOff.setModifiedBy((String) oJDetl.get("sModified") );
            if (oJDetl.get("dModified") != null) oOff.setDateModified(SQLUtil.toDate((String) oJDetl.get("dModified"), SQLUtil.FORMAT_TIMESTAMP));
            loArray.add(oOff);
        }
        
        return loArray;
    }

    private boolean pbWithParnt = false;
    private String psBranchCD = "";
    private String psUserIDxx = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private GRider poGRider = null;
    private Date poTransact = null;

}

