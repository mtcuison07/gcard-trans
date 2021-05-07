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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GTransaction;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.integsys.pojo.UnitGCHistory;

/**
 *
 * @author kalyptus
 * Kalyptus [06/04/2010 12:53 pm]
 *    - started creating this code
 */

public class GCHistory implements GTransaction {
    public Object newTransaction() {
       UnitGCHistory loOcc = new UnitGCHistory();

       if(psBranchCD.equals("")) {
          psBranchCD = poGRider.getBranchCode();
       }

       loOcc.setGCardNo(psGCardNox);

       return loOcc;
    }

    //fsTransNox = "dTransact»cCardStat
    public Object loadTransaction(String fsTransNox) {
       UnitGCHistory loOcc = new UnitGCHistory();
       Connection loCon = poGRider.getConnection();

      //retrieve the record
       String []lasTrans = fsTransNox.split("»");
       StringBuilder lsSQL = new StringBuilder();
       lsSQL.append(getSQ_Master());
       lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(psGCardNox));

       if(lasTrans.length > 0){
          lsSQL.append(" AND dTransact = " + SQLUtil.toSQL(lasTrans[0]));
          lsSQL.append(" AND cCardStat = " + SQLUtil.toSQL(lasTrans[1]));
       }

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

       return loOcc;
    }

    //fsTransNox = "dTransact»cCardStat
    public Object saveUpdate(Object foEntity, String fsTransNox) {
       String lsSQL = "";
       UnitGCHistory loOldEnt = null;
       UnitGCHistory loNewEnt = null;
       UnitGCHistory loResult = null;

       // Check for the value of foEntity
       if (!(foEntity instanceof UnitGCHistory)) {
           setErrMsg("Invalid Entity Passed as Parameter");
           return loResult;
       }

       // Typecast the Entity to this object
       loNewEnt = (UnitGCHistory) foEntity;

       //TODO: Test the user rights in these area...

       //Set the value of sModified and dModified here
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
          //Generate the INSERT statement
           lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt);
       } else {
           //Reload previous record
           loOldEnt = (UnitGCHistory) loadTransaction(fsTransNox);
           //Generate the UPDATE statement

           String []lasTrans = fsTransNox.split("»");
           lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt, (GEntity)loOldEnt, 
                        "sGCardNox = " + SQLUtil.toSQL(psGCardNox) +
                   " AND dTransact = " + SQLUtil.toSQL(lasTrans[0]) +
                   " AND cCardStat = " + SQLUtil.toSQL(lasTrans[1]));
       }

       //No changes has been made
       if (lsSQL.equals("")) {
           setMessage("Record is not updated!");
           return loResult;
       }

       if(!pbWithParnt)
          poGRider.beginTrans();

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

    //fsTransNox = "dTransact»cCardStat
    public boolean deleteTransaction(String fsTransNox) {
       UnitGCHistory  loOcc = (UnitGCHistory) loadTransaction(fsTransNox);
       boolean lbResult = false;

       if(loOcc == null){
          setMessage("No record found!");
          return lbResult;
       }

       //TODO: Test the user rights in these area...

       String []lasTrans = fsTransNox.split("»");

       StringBuilder lsSQL = new StringBuilder();
       lsSQL.append("DELETE FROM " + loOcc.getTable());
       lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(psGCardNox));

       if(lasTrans.length > 0){
          lsSQL.append(" AND dTransact = " + SQLUtil.toSQL(lasTrans[0]));
          lsSQL.append(" AND cCardStat = " + SQLUtil.toSQL(lasTrans[1]));
       }

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

    //Used in verifying the offline transaction
    //fsTransNox = "dTransact»cCardStat
    public boolean closeTransaction(String fsTransNox) {
       UnitGCHistory loOcc = (UnitGCHistory) loadTransaction(fsTransNox);
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
       String []lasTrans = fsTransNox.split("»");

       StringBuilder lsSQL = new StringBuilder();

       lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
       lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED));
       lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
       lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
       lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(psGCardNox));
       if(lasTrans.length > 0){
          lsSQL.append(" AND dTransact = " + SQLUtil.toSQL(lasTrans[0]));
          lsSQL.append(" AND cCardStat = " + SQLUtil.toSQL(lasTrans[1]));
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

    public boolean postTransaction(String fsTransNox) {
       UnitGCHistory loOcc = (UnitGCHistory) loadTransaction(fsTransNox);
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

       //TODO: Write the information to the GCard here

       //GCrypt loCrypt = new GCrypt();
       String []lasTrans = fsTransNox.split("»");

       StringBuilder lsSQL = new StringBuilder();

       lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
       lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_POSTED));
       lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
       lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
       lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(psGCardNox));
       if(lasTrans.length > 0){
          lsSQL.append(" AND dTransact = " + SQLUtil.toSQL(lasTrans[0]));
          lsSQL.append(" AND cCardStat = " + SQLUtil.toSQL(lasTrans[1]));
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

    public boolean voidTransaction(String fsTransNox) {
       UnitGCHistory loOcc = (UnitGCHistory) loadTransaction(fsTransNox);
       boolean lbResult = false;

       if(loOcc == null){
          setMessage("No record found!");
          return lbResult;
       }

       //exit if transaction is not in CLOSED status
       if(!(loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED)||
            loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED))){
          setMessage("Current record is neither in CLOSED nor POSTED status!");
          return lbResult;
       }

       //TODO: Test the user rights in these area...


       //Test if transaction was posted to the GCard
       if(loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED)){
          //TODO: Undo the posting to the GCard here
       }

       //GCrypt loCrypt = new GCrypt();
       String []lasTrans = fsTransNox.split("»");

       StringBuilder lsSQL = new StringBuilder();

       lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
       lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED));
       lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
       lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
       lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(psGCardNox));
       if(lasTrans.length > 0){
          lsSQL.append(" AND dTransact = " + SQLUtil.toSQL(lasTrans[0]));
          lsSQL.append(" AND cCardStat = " + SQLUtil.toSQL(lasTrans[1]));
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
        UnitGCHistory loOcc = (UnitGCHistory) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
            setMessage("No record found!");
            return lbResult;
        }

        //exit if transaction is not in CLOSED status
        if(!(loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED)||
             loOcc.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED))){
            setMessage("Current record is neither in CLOSED nor POSTED status!");
            return lbResult;
        }

        //GCrypt loCrypt = new GCrypt();
        String []lasTrans = fsTransNox.split("»");

        StringBuilder lsSQL = new StringBuilder();

        lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
        lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED));
        lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
        lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
        lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(psGCardNox));
        if(lasTrans.length > 0){
           lsSQL.append(" AND dTransact = " + SQLUtil.toSQL(lasTrans[0]));
           lsSQL.append(" AND cCardStat = " + SQLUtil.toSQL(lasTrans[1]));
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

    // if isall is false load only close transaction to post points to the card
    public ArrayList<UnitGCHistory> loadLedger() {
        UnitGCHistory loOcc = null;
        Connection loCon = null;
        ArrayList<UnitGCHistory> loArray = new ArrayList<UnitGCHistory>();

        loCon = poGRider.getConnection();

        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append(getSQ_Master());
        lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(this.psGCardNox));

        Statement loStmt = null;
        ResultSet loRS = null;
        try {
            loStmt = loCon.createStatement();
            loRS = loStmt.executeQuery(lsSQL.toString());

            while(loRS.next()){
                loOcc = new UnitGCHistory();
                //load each column to the entity
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loOcc.setValue(lnCol, loRS.getObject(lnCol));
                }

                loArray.add(loOcc);
            }
        } catch (SQLException ex) {
            Logger.getLogger(GCApplication.class.getName()).log(Level.SEVERE, null, ex);
            setErrMsg(ex.getMessage());
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        return loArray;
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
        return (MiscUtil.makeSelect(new UnitGCHistory()));
    }

     // add methods here
    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
        this.psUserIDxx = foGRider.getUserID();
        if(psBranchCD.isEmpty())
            psBranchCD = poGRider.getBranchCode();
    }

    public void setGCardNo(String fsGCardNo) {
        this.psGCardNox = fsGCardNo;
    }

    public String getGCardNo() {
        return this.psGCardNox;
    }

    private boolean pbWithParnt = false;
    private String psBranchCD = "";
    private String psUserIDxx = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private GRider poGRider = null;
    private String psGCardNox = "";
}

