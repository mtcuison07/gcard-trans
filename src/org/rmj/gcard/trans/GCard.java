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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.constants.GCardStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GRecord;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.integsys.pojo.UnitGCHistory;
import org.rmj.integsys.pojo.UnitGCard;

/**
 *
 * @author kalyptus
 */
public class GCard implements GRecord {
   public Object newRecord() {
      System.out.println("GCard.newRecord");
      UnitGCard  loOcc = new UnitGCard();
      Connection loCon = poGRider.getConnection();

      loOcc.setGCardNo(MiscUtil.getNextCode(loOcc.getTable(), "sGCardNox", true, loCon, psBranchCD));

      return loOcc;
   }

   public Object openRecord(String fstransNox) {
      System.out.print("GCard.openRecord");
      UnitGCard  loOcc = new UnitGCard();
      Connection loCon = poGRider.getConnection();

      //retrieve the record
      String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sGCardNox = " + SQLUtil.toSQL(fstransNox));
      Statement loStmt = null;
      ResultSet loRS = null;
      try {
         loStmt = loCon.createStatement();
         System.out.println(lsSQL);
         loRS = loStmt.executeQuery(lsSQL);

         if(!loRS.next())
             setMessage("No Old Record Found!");
         else{
            //load each column to the entity
            for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                loOcc.setValue(lnCol, loRS.getObject(lnCol));
            }
         }
      } catch (SQLException ex) {
         Logger.getLogger(GCard.class.getName()).log(Level.SEVERE, null, ex);
         setErrMsg(ex.getMessage());
      }
      finally{
         MiscUtil.close(loRS);
         MiscUtil.close(loStmt);
      }

      return loOcc;
   }

   public Object open2Card(String fstransNox) {
      System.out.print("GCard.openRecord");
      UnitGCard  loOcc = new UnitGCard();
      Connection loCon = poGRider.getConnection();

      //retrieve the record
      String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sCardNmbr = " + SQLUtil.toSQL(fstransNox));
      Statement loStmt = null;
      ResultSet loRS = null;
      try {
         loStmt = loCon.createStatement();
         System.out.println(lsSQL);
         loRS = loStmt.executeQuery(lsSQL);

         if(!loRS.next())
             setMessage("No Old Record Found!");
         else{
            //load each column to the entity
            for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                loOcc.setValue(lnCol, loRS.getObject(lnCol));
            }
         }
      } catch (SQLException ex) {
         Logger.getLogger(GCard.class.getName()).log(Level.SEVERE, null, ex);
         setErrMsg(ex.getMessage());
      }
      finally{
         MiscUtil.close(loRS);
         MiscUtil.close(loStmt);
      }

      return loOcc;
   }
   
   public Object saveRecord(Object foEntity, String fsTransNox) {
      System.out.println("GCard.saveRecord");
      String lsSQL = "";
      UnitGCard loOldEnt = null;
      UnitGCard loNewEnt = null;
      UnitGCard loResult = null;

      // Check for the value of foEntity
      if (!(foEntity instanceof UnitGCard)) {
          setErrMsg("Invalid Entity Passed as Parameter");
          return loResult;
      }

      // Typecast the Entity to this object
      loNewEnt = (UnitGCard) foEntity;

      //test for the validity of the different fields here
      if (loNewEnt.getClientID().equals("")) {
          setMessage("Invalid Customer Detected");
          return loResult;
      }

//      if (loNewEnt.getCompanyID().equals("")) {
//          setMessage("Invalid Company Detected");
//          return loResult;
//      }

      //TODO: Test the user rights in these area...

      //Set the value of sModified and dModified here
      //GCrypt loCrypt = new GCrypt();
      //loNewEnt.setModifiedBy(loCrypt.encrypt(psUserIDxx));
      loNewEnt.setModifiedBy(psUserIDxx);
      loNewEnt.setDateModified(poGRider.getServerDate());

      //Generate the SQL Statement
      if (fsTransNox.equals("")) {
         //TODO: Get new id for this record...
         Connection loCon = poGRider.getConnection();

         loNewEnt.setValue(1, MiscUtil.getNextCode(loNewEnt.getTable(), "sGCardNox", true, loCon, psBranchCD));

         if(!pbWithParnt)
            MiscUtil.close(loCon);

         //Generate the INSERT statement
          lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt);
      } else {
          //Reload previous record
          loOldEnt = (UnitGCard) openRecord(fsTransNox);
          //Generate the UPDATE statement
          lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt, (GEntity)loOldEnt, "sGCardNox = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
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
         loResult = loNewEnt ;

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return loResult;
   }

   // Deletion of GCard will not be implemented
   public boolean deleteRecord(String fsTransNox) {
      throw new UnsupportedOperationException("deleteRecord method is not supported.");
   }

   // use suspend
   public boolean deactivateRecord(String fsTransNox) {
      throw new UnsupportedOperationException("deactivateRecord method is not supported yet.");
   }

   // use activate
   public boolean activateRecord(String fsTransNox) {
      throw new UnsupportedOperationException("activateRecord method is not supported yet.");
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
      return (MiscUtil.makeSelect(new UnitGCard()));
   }

// add methods here
   public void setGRider(GRider foGRider) {
      this.poGRider = foGRider;
      this.psUserIDxx = foGRider.getUserID();
      if(psBranchCD.isEmpty())
         psBranchCD = poGRider.getBranchCode();

   }

   //fsRemarks refers to the serial of the GCard retrieve from the GCard chip
   public boolean print(String fsTransNox, String fsRemarks, Date date){
      UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      if(!loOcc.getCardStat().equals(GCardStatus.NEW)){
         setMessage("Only recently created cards can be printed!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      //print the GCard here(if printing is supported)

      //GCrypt loCrypt = new GCrypt();
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
      lsSQL.append("  cCardStat = " + SQLUtil.toSQL(GCardStatus.PRINTED));
      lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
      lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
      lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(fsTransNox));

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         if(saveHistory(fsTransNox, date, fsRemarks, GCardStatus.PRINTED)){
            GCApplication loApp = new GCApplication();
            loApp.setGRider(poGRider);
            loApp.setBranch(psBranchCD);
            loApp.setWithParent(true);
            lbResult = loApp.postTransaction(loOcc.getApplicNo());
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

   // set the status to encode 
   public boolean encode(String fsTransNox, Date date){
      UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      if(!loOcc.getCardStat().equals(GCardStatus.PRINTED)){
         setMessage("Only recently printed cards can be encoded!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      //GCrypt loCrypt = new GCrypt();
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
      lsSQL.append("  cCardStat = " + SQLUtil.toSQL(GCardStatus.ENCODED));
      lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
      lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
      lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(fsTransNox));

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         if(saveHistory(fsTransNox, date, "", GCardStatus.ENCODED))
             lbResult = true;
      }

      System.out.println("Before writing information to GCard");
      if(lbResult){
          //TODO: Write the information to the GCard here
          System.out.println("Writing information to GCard");
      }

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }

    public boolean issue(String fsTransNox, Date date){
        UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
            setMessage("No record found!");
            return lbResult;
        }

        if (loOcc.getDigital().equals("2")){
            if(!loOcc.getCardStat().equals(GCardStatus.PRINTED)){
                setMessage("Only recently printed cards can be issued!");
                return lbResult;
            }
        } else {
            if(!loOcc.getCardStat().equals(GCardStatus.ENCODED)){
                setMessage("Only recently encoded cards can be issued!");
                return lbResult;
            }
        }
      //TODO: Test the user rights in these area...

      //GCrypt loCrypt = new GCrypt();
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
      lsSQL.append("  cCardStat = " + SQLUtil.toSQL(GCardStatus.ISSUED));
      lsSQL.append(", cLocation = " + SQLUtil.toSQL("3"));
      lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
      lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
      lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(fsTransNox));

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         if(saveHistory(fsTransNox, date, "", GCardStatus.ISSUED))
             lbResult = true;
      }

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }

   //set the status to replaced
   public boolean activate(String fsTransNox, Date date){
      UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }
      System.out.println(loOcc.getCardStat());
      
      if(!loOcc.getCardStat().equals(GCardStatus.ISSUED)){
         if(loOcc.getCardStat().equals(GCardStatus.SUSPENDED)){
            if(loOcc.getExpiryDate().compareTo(date) < 0 ){
               setMessage("Expired cards can not be reactivated!");
               return lbResult;
            }
         }
         else{
            setMessage("Only recently printed cards can be activated!");
            return lbResult;
         }
      }

      //TODO: Test the user rights in these area...

      //GCrypt loCrypt = new GCrypt();
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
      lsSQL.append("  cCardStat = " + SQLUtil.toSQL(GCardStatus.ACTIVATED));
      lsSQL.append(", dActivate = " + SQLUtil.toSQL(SQLUtil.dateFormat(date, "yyyy-MM-dd")));
      lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
      lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
      lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(fsTransNox));

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         if(saveHistory(fsTransNox, date, "", GCardStatus.ACTIVATED))
             lbResult = true;
      }

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }

   //set the status to replaced
   public boolean suspend(String fsTransNox, String fsRemarks, Date date){
      UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      if(!loOcc.getCardStat().equals(GCardStatus.ACTIVATED)){
         setMessage("Only recently activated cards can be suspended!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      //GCrypt loCrypt = new GCrypt();
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
      lsSQL.append("  cCardStat = " + SQLUtil.toSQL(GCardStatus.SUSPENDED));
      lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
      lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
      lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(fsTransNox));

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         if(saveHistory(fsTransNox, date, fsRemarks, GCardStatus.SUSPENDED))
             lbResult = true;
      }

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }

   //set the status to replaced
   public boolean replace(String fsTransNox, Date date){
      UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      if(!(loOcc.getCardStat().equals(GCardStatus.ACTIVATED) || loOcc.getCardStat().equals(GCardStatus.SUSPENDED))){
         setMessage("Only active/suspended cards can be replaced!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      //GCrypt loCrypt = new GCrypt();
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
      lsSQL.append("  cCardStat = " + SQLUtil.toSQL(GCardStatus.REPLACED));
      lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
      lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
      lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(fsTransNox));

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         if(saveHistory(fsTransNox, date, "", GCardStatus.REPLACED))
             lbResult = true;
      }

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }
   
   //set the status to renew
   public boolean renew(String fsTransNox, Date date){
      UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      if(!(loOcc.getCardStat().equals(GCardStatus.ACTIVATED) || loOcc.getCardStat().equals(GCardStatus.SUSPENDED))){
         setMessage("Only active/suspended cards can be renewed!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      //GCrypt loCrypt = new GCrypt();
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
      lsSQL.append("  cCardStat = " + SQLUtil.toSQL(GCardStatus.RENEWED));
      lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
      lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
      lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(fsTransNox));

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         if(saveHistory(fsTransNox, date, "", GCardStatus.RENEWED))
             lbResult = true;
      }

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }

   public boolean issue_petron_value_card(String fsTransNox, Date date, String fsPetronNo){
      UnitGCard  loOcc = (UnitGCard) openRecord(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      StringBuilder lsSQL = new StringBuilder();
      StringBuilder lsNme = new StringBuilder();
      
      lsNme.append("(sGCardNox");
      lsNme.append(", sPetronNo");
      lsNme.append(", dReleased");
      lsNme.append(", sReleased)");
      
      lsSQL.append("( " + SQLUtil.toSQL(fsTransNox)); 
      lsSQL.append(", " + SQLUtil.toSQL(fsPetronNo)); 
      lsSQL.append(", " + SQLUtil.toSQL(date)); 
      lsSQL.append(", " + SQLUtil.toSQL(psUserIDxx)); 
      lsSQL.append(")");
      
      if(!pbWithParnt)
         poGRider.beginTrans();

      System.out.println(lsSQL.toString());
      
      if(poGRider.executeQuery("INSERT INTO G_Card_Petron " + lsNme.toString() + " VALUES " + lsSQL.toString(), "G_Card_Petron", "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record updated");
      }
      else{
         lbResult = true;
      }

      if(!pbWithParnt){
         if(getErrMsg().isEmpty())
            poGRider.commitTrans();
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }
   
   private boolean saveHistory(String card, Date date, String string, String stat){
     GCHistory loHist = new GCHistory();
     loHist.setGRider(poGRider);
     loHist.setWithParent(true);
     loHist.setGCardNo(card);
     loHist.setBranch(psBranchCD);
     UnitGCHistory loUnitHist = (UnitGCHistory) loHist.newTransaction();
     loUnitHist.setTransactDate(date);
     loUnitHist.setRemarks(string);
     loUnitHist.setCardStat(stat);

     UnitGCHistory loUnitHist1 = (UnitGCHistory) loHist.saveUpdate(loUnitHist, "");

     boolean isOk = !loUnitHist1.getGCardNo().isEmpty();

     //if not saved to history update the error message...
     if(!isOk){
         if(!loHist.getErrMsg().isEmpty())
           setErrMsg(loHist.getErrMsg());
         else
           setMessage(loHist.getMessage());
     }

     System.out.println("History result: " + isOk);
     return isOk;
   }

   private boolean pbWithParnt = false;
   private String psBranchCD = "";
   private String psUserIDxx = "";
   private String psWarnMsg = "";
   private String psErrMsgx = "";
   private GRider poGRider = null;
}
