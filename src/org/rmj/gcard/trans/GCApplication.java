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
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.constants.GCardStatus;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.GCrypt;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GTransaction;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.integsys.pojo.UnitGCApplication;
import org.rmj.integsys.pojo.UnitGCard;

/**
 *
 * @author kalyptus
 * Jheff [ 05/25/2010 09:10 am ]
 *    - start coding this object.
 */
public class GCApplication implements GTransaction {
   public Object newTransaction() {
      UnitGCApplication loOcc = new UnitGCApplication();
      Connection loCon = poGRider.getConnection();

      loOcc.setTransNo(MiscUtil.getNextCode(loOcc.getTable(), "sTransNox", true, loCon, psBranchCD));

      loOcc.setPoints(0.00);
      return loOcc;
   }

   public Object loadTransaction(String fsTransNox) {
      System.out.print("GCApplication.loadTransaction");
      UnitGCApplication loOcc = new UnitGCApplication();
      Connection loCon = poGRider.getConnection();
      //retrieve the record
      String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
      Statement loStmt = null;
      ResultSet loRS = null;
      try {
         loStmt = loCon.createStatement();
         loRS = loStmt.executeQuery(lsSQL);
         
         if(!loRS.next())
             setMessage("loadTransaction: No Record Found!" + lsSQL);
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
      }
      return loOcc;
   }

   public Object saveUpdate(Object foEntity, String fsTransNox) {
        String lsSQL = "";
        UnitGCApplication loOldEnt = null;
        UnitGCApplication loNewEnt = null;
        UnitGCApplication loResult = null;

        // Check for the value of foEntity
        if (!(foEntity instanceof UnitGCApplication)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return null;
        }

        // Typecast the Entity to this object
        loNewEnt = (UnitGCApplication) foEntity;
        loNewEnt.setModifiedBy(psUserIDxx);
        loNewEnt.setDateModified(poGRider.getServerDate());

        //Generate the SQL Statement
        if (fsTransNox.equals("")) {
            Connection loCon = poGRider.getConnection();

            loNewEnt.setValue(1, MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loCon, psBranchCD));

           //Generate the INSERT statement
            lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt);
            //lsSQL = MiscUtil.makeSQLite((GEntity)loNewEnt);
        } else {
            //Reload previous record
            loOldEnt = (UnitGCApplication) loadTransaction(fsTransNox);
            //Generate the UPDATE statement
            lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt, (GEntity)loOldEnt, "sTransNox = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
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

    public boolean deleteTransaction(String fsTransNox) {
        UnitGCApplication  loOcc = (UnitGCApplication) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
            setMessage("No record found!");
            return lbResult;
        }

        //TODO: Test the user rights in these area...

        //Test if application is open
        if(!loOcc.getTranStat().equals(TransactionStatus.STATE_OPEN)){
            setMessage("Processing is in progress. Deletion is not allowed!");
            return lbResult;
        }

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
    
    //mac 2020.06.20
    // use this in setting the application as verified for non-chip card
    // use this in setting the application as verified
    public boolean closeTransaction(String fsTransNox, String fsCardNmbr) {
        System.out.println("GCApplication.closeTransaction");
        UnitGCApplication  loOcc = (UnitGCApplication) loadTransaction(fsTransNox);
        boolean lbResult = true;
        UnitGCard loUGCardx;

        psGCardNox = "";

        if(loOcc == null){
            setMessage("No record found!");
            return false;
        }

        if(!loOcc.getTranStat().equals(TransactionStatus.STATE_OPEN)){
            setMessage("Only unverified transaction can be verified.");
            return false;
        }

        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
        lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED));
        lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
        lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        if(!pbWithParnt)
            poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
            lbResult = false;
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
                setMessage("No record updated");
        }
        else{
            //Upon verification create the GCard
            GCard loGCard = new GCard();
            loGCard.setGRider(poGRider);
            loGCard.setWithParent(true);
            loGCard.setBranch(loOcc.getCompanyID());
            UnitGCard loUGCard = (UnitGCard) loGCard.newRecord();
            loUGCard.setClientID(loOcc.getClientID());
            loUGCard.setApplicNo(loOcc.getTransNo());
            loUGCard.setCardType(loOcc.getCardType());
            loUGCard.setDateActivated(loOcc.getTransactDate());
            loUGCard.setCompanyID(loOcc.getCompanyID());
            loUGCard.setExpiryDate(MiscUtil.dateAdd(loOcc.getTransactDate(), Calendar.YEAR, 2));
            //Generate Card Number
            loUGCard.setCardNumber(fsCardNmbr);
            //Autogenerate random pin number
            loUGCard.setPINumber("");
            loUGCard.setLocation("0");
            loUGCard.setDigital("2");

            //If new
            if(loOcc.getApplicationType().equalsIgnoreCase("1")){
                loUGCard.setMemberSince(loOcc.getTransactDate());

                //set the balance for the card
                loUGCard.setAvailablePoints(loOcc.getPoints());
                loUGCard.setTotalPoints(loOcc.getPoints());
                loUGCard.setPoints(loOcc.getPoints());
            }   
            else{
                GCard loGCardx = new GCard();
                loGCardx.setGRider(poGRider);
                loGCardx.setWithParent(true);

                loUGCardx = (UnitGCard) loGCard.open2Card(loOcc.getPreviousGCard());

                System.out.println("Examining GCard Length");
                if(loUGCardx.getGCardNo().isEmpty()){
                    System.out.println(loOcc.getPreviousGCard() + " is below the expected length");
                    setErrMsg("Invalid GCard Length");
                    lbResult = false; 
                }
                else{
                    //get membership date from the Previous GCard Number
                    loUGCard.setMemberSince(loUGCardx.getMemberSince());

                    if(loUGCardx.getCardStat().equalsIgnoreCase(GCardStatus.ACTIVATED)){
                        float lnPointDed = (float)(loOcc.getPurchaseMode().equalsIgnoreCase("4") ? loOcc.getAmountPaid() : 0);   

                        loUGCard.setAvailablePoints(loUGCardx.getAvailablePoints() - lnPointDed);
                        loUGCard.setTotalPoints(loUGCardx.getTotalPoints() - lnPointDed);
                        loUGCard.setPoints(loUGCardx.getPoints() - lnPointDed);

                        //Is is replacement
                        if(loOcc.getApplicationType().equals("0")){
                            if (!loGCardx.replace(loUGCardx.getGCardNo(), loOcc.getTransactDate())){
                                loGCard.setErrMsg(loGCardx.getErrMsg());
                                loGCard.setMessage(loGCardx.getMessage());
                                lbResult = false;
                            }
                        } 
                        //could be 3 meaning renewal
                        else{
                            if (!loGCardx.renew(loUGCardx.getGCardNo(), loOcc.getTransactDate())){
                                loGCard.setErrMsg(loGCardx.getErrMsg());
                                loGCard.setMessage(loGCardx.getMessage());
                                lbResult = false;
                            }
                        }
                    }
                    else{
                       loUGCard.setAvailablePoints(0.00);
                       loUGCard.setTotalPoints(0.00);
                       loUGCard.setPoints(0.00);
                    }
                }
            }
            //save GCard
            if(lbResult){
               loUGCard = (UnitGCard) loGCard.saveRecord(loUGCard,"");
               psGCardNox = loUGCard.getGCardNo();
            }   

            if(loUGCard == null || lbResult == false){
               setErrMsg(loGCard.getErrMsg());
               setMessage(loGCard.getMessage());
               lbResult = false;
            }
            
            //mac 2020.06.23
            //  update the gcard status to printed
            GCard loGCardx = new GCard();
            loGCardx.setGRider(poGRider);
            loGCardx.setWithParent(true);
            if (!loGCardx.print(psGCardNox, "", poGRider.getServerDate())){
                 setErrMsg(loGCardx.getErrMsg());
                setMessage(loGCardx.getMessage());
                lbResult = false;
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
    
    //mac 2020.06.05
    //  if the customer opt to release the card upon verification, use this method
    public boolean releaseTransaction(){
        if (psGCardNox.isEmpty()){
            setMessage("G-Card number is empty.");
            return false;
        }
        
        GCard loGCardx = new GCard();
        loGCardx.setGRider(poGRider);
        
        //  update the card status to released
        if (!loGCardx.issue(psGCardNox, poGRider.getServerDate())){
            setErrMsg(loGCardx.getErrMsg());
            setMessage(loGCardx.getMessage());
            return false;
        }

        //  update the card status to activated
        if (!loGCardx.activate(psGCardNox, poGRider.getServerDate())){
            setErrMsg(loGCardx.getErrMsg());
            setMessage(loGCardx.getMessage());
            return false;
        }
        
        return true;
    }
    
    // use this in setting the application as verified
    public boolean closeTransaction(String fsTransNox) {
        System.out.println("GCApplication.closeTransaction");
        UnitGCApplication  loOcc = (UnitGCApplication) loadTransaction(fsTransNox);
        boolean lbResult = true;
        UnitGCard loUGCardx;

        psGCardNox = "";

        if(loOcc == null){
            setMessage("No record found!");
            return false;
        }

        if(!loOcc.getTranStat().equals(TransactionStatus.STATE_OPEN)){
            setMessage("Only unverified transaction can be verified.");
            return false;
        }

        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
        lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED));
        lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
        lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        if(!pbWithParnt)
            poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
            lbResult = false;
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
                setMessage("No record updated");
        }
        else{
            //Upon verification create the GCard
            GCard loGCard = new GCard();
            loGCard.setGRider(poGRider);
            loGCard.setWithParent(true);
            loGCard.setBranch(loOcc.getCompanyID());
            UnitGCard loUGCard = (UnitGCard) loGCard.newRecord();
            loUGCard.setClientID(loOcc.getClientID());
            loUGCard.setApplicNo(loOcc.getTransNo());
            loUGCard.setCardType(loOcc.getCardType());
            loUGCard.setDateActivated(loOcc.getTransactDate());
            loUGCard.setCompanyID(loOcc.getCompanyID());
            loUGCard.setExpiryDate(MiscUtil.dateAdd(loOcc.getTransactDate(), Calendar.YEAR, 2));
            //Generate Card Number
            loUGCard.setCardNumber(createGCardNumber(loOcc.getCompanyID(), loOcc.getCardType()));
            //Autogenerate random pin number
            loUGCard.setPINumber(createPinNumber());
            loUGCard.setLocation("0");

            //If new
            if(loOcc.getApplicationType().equalsIgnoreCase("1")){
                loUGCard.setMemberSince(loOcc.getTransactDate());

                //set the balance for the card
                loUGCard.setAvailablePoints(loOcc.getPoints());
                loUGCard.setTotalPoints(loOcc.getPoints());
                loUGCard.setPoints(loOcc.getPoints());
            }   
            else{
                GCard loGCardx = new GCard();
                loGCardx.setGRider(poGRider);
                loGCardx.setWithParent(true);

                loUGCardx = (UnitGCard) loGCard.open2Card(loOcc.getPreviousGCard());

                System.out.println("Examining GCard Length");
                if(loUGCardx.getGCardNo().isEmpty()){
                    System.out.println(loOcc.getPreviousGCard() + " is below the expected length");
                    setErrMsg("Invalid GCard Length");
                    lbResult = false; 
                }
                else{
                    //get membership date from the Previous GCard Number
                    loUGCard.setMemberSince(loUGCardx.getMemberSince());

                    if(loUGCardx.getCardStat().equalsIgnoreCase(GCardStatus.ACTIVATED)){
                        float lnPointDed = (float)(loOcc.getPurchaseMode().equalsIgnoreCase("4") ? loOcc.getAmountPaid() : 0);   

                        loUGCard.setAvailablePoints(loUGCardx.getAvailablePoints() - lnPointDed);
                        loUGCard.setTotalPoints(loUGCardx.getTotalPoints() - lnPointDed);
                        loUGCard.setPoints(loUGCardx.getPoints() - lnPointDed);

                        //Is is replacement
                        if(loOcc.getApplicationType().equals("0")){
                            if (!loGCardx.replace(loUGCardx.getGCardNo(), loOcc.getTransactDate())){
                                loGCard.setErrMsg(loGCardx.getErrMsg());
                                loGCard.setMessage(loGCardx.getMessage());
                                lbResult = false;
                            }
                        } 
                        //could be 3 meaning renewal
                        else{
                            if (!loGCardx.renew(loUGCardx.getGCardNo(), loOcc.getTransactDate())){
                                loGCard.setErrMsg(loGCardx.getErrMsg());
                                loGCard.setMessage(loGCardx.getMessage());
                                lbResult = false;
                            }
                        }
                    }
                    else{
                       loUGCard.setAvailablePoints(0.00);
                       loUGCard.setTotalPoints(0.00);
                       loUGCard.setPoints(0.00);
                    }
                }
            }
            //save GCard
            if(lbResult){
               loUGCard = (UnitGCard) loGCard.saveRecord(loUGCard,"");
               psGCardNox = loUGCard.getGCardNo();
            }   

            if(loUGCard == null || lbResult == false){
               setErrMsg(loGCard.getErrMsg());
               setMessage(loGCard.getMessage());
               lbResult = false;
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

    //use this in setting the application as 
    public boolean postTransaction(String fsTransNox) {
        UnitGCApplication  loOcc = (UnitGCApplication) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
            setMessage("No record found!");
            return lbResult;
        }

        if(!loOcc.getTranStat().equals(TransactionStatus.STATE_CLOSED)){
           setMessage("Only verified application can be printed.");
           return lbResult;
        }

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

    public boolean voidTransaction(String fsTransNox) {
        UnitGCApplication  loOcc = (UnitGCApplication) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
            setMessage("No record found!");
            return lbResult;
        }

        //TODO: Test the user rights in these area...

        //Test if application is open
        if(!loOcc.getTranStat().equals(TransactionStatus.STATE_OPEN)){
            StringBuilder status = new StringBuilder();
            if(loOcc.getTranStat().equals(TransactionStatus.STATE_CLOSED))
                status.append("VERIFIED");
            else if(loOcc.getTranStat().equals(TransactionStatus.STATE_POSTED))
                status.append("PRINTED/POSTED");
            else if(loOcc.getTranStat().equals(TransactionStatus.STATE_CANCELLED))
                status.append("CANCELLED");
            else if(loOcc.getTranStat().equals(TransactionStatus.STATE_VOID))
                status.append("VOID");

            setMessage("Application has been tagged " + status.toString() + ". Setting this application to void is not allowed!");
            return lbResult;
        }

        //GCrypt loCrypt = new GCrypt();
        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
        lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_VOID));
        lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
        lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        System.out.println(pbWithParnt);

        if(!pbWithParnt)
            poGRider.beginTrans();

        System.out.println(lsSQL.toString());
        System.out.println(loOcc.getTable());
        System.out.println(fsTransNox.substring(0, 2));

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

    public boolean cancelTransaction(String fsTransNox) {
        UnitGCApplication  loOcc = (UnitGCApplication) loadTransaction(fsTransNox);
        boolean lbResult = false;

        if(loOcc == null){
            setMessage("No record found!");
            return lbResult;
        }

        //TODO: Test the user rights in these area...


        //Test if application is open
        if(!loOcc.getTranStat().equals(TransactionStatus.STATE_OPEN)){
            StringBuilder status = new StringBuilder();
            if(loOcc.getTranStat().equals(TransactionStatus.STATE_CLOSED))
                status.append("VERIFIED");
            else if(loOcc.getTranStat().equals(TransactionStatus.STATE_POSTED))
                status.append("PRINTED/POSTED");
            else if(loOcc.getTranStat().equals(TransactionStatus.STATE_CANCELLED))
                status.append("CANCELLED");
            else if(loOcc.getTranStat().equals(TransactionStatus.STATE_VOID))
                status.append("VOID");

            setMessage("Application has been tagged " + status.toString() + ". Setting this application to void is not allowed!");
            return lbResult;
        }

        //GCrypt loCrypt = new GCrypt();
        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append("UPDATE " + loOcc.getTable() + " SET ");
        lsSQL.append("  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED));
        lsSQL.append(", sModified = " + SQLUtil.toSQL(psUserIDxx));
        lsSQL.append(", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()));
        lsSQL.append(" WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox));

        if(!pbWithParnt)
            poGRider.beginTrans();

        if(poGRider.executeQuery(lsSQL.toString(), loOcc.getTable(), "", "") == 0){
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
                setMessage("No record was cancelled.");
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

    private String createGCardNumber(String fsBranch, String fcType){
        System.out.append("GApplication.createGCardNumber");
        StringBuilder lsGCard = new StringBuilder();
        //set the branch part
        //if branch code is more than 2 characters... it means 
        //it is using the new branch code format...

        //we only need the last 3 parts in creating the new structure
        if(fsBranch.length() > 2){
           //remove the company indicator ['C' or 'M'] 
           lsGCard.append(fsBranch.substring(fsBranch.length() - 3));
        }
        else
           lsGCard.append(fsBranch); 

        //lsGCard.append(SQLUtil.dateFormat(poGRider.getSysDate(), "yy"));
        lsGCard.append(SQLUtil.dateFormat(poGRider.getServerDate(), "yy"));

        //set the series part
        String lsSQL = "SELECT sCardNmbr" +
                      " FROM G_Card_Master" +
                      " WHERE sCardNmbr LIKE " + SQLUtil.toSQL(lsGCard.toString() + "%") +
                      " ORDER BY sCardNmbr DESC" +
                      " LIMIT 1";
        Connection loCon = poGRider.getConnection();
        Statement loStmt = null;
        ResultSet loRS = null;
        try {
            loStmt = loCon.createStatement();
            loRS = loStmt.executeQuery(lsSQL);

            if(!loRS.next())
                lsGCard.append(String.format("%06d", 1));
            else{
                StringBuilder lsValue = new StringBuilder();
                if(fsBranch.length() == 2) 
                    lsValue.append(loRS.getString("sCardNmbr").substring(4, 10));
                else
                    lsValue.append(loRS.getString("sCardNmbr").substring(5, 11));

                System.out.println("Series: " + lsValue + "»" + (Integer.parseInt(lsValue.toString()) + 1));
                lsGCard.append(String.format("%06d", Integer.parseInt(lsValue.toString()) + 1));
            }
        } catch (SQLException ex) {
            Logger.getLogger(GCApplication.class.getName()).log(Level.SEVERE, null, ex);
            setErrMsg(ex.getMessage());
            lsGCard = new StringBuilder();
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }

        //set the tr part
        if(lsGCard.length() > 0){
            lsGCard.append(fcType);

            Random loRnd = new Random();
            lsGCard.append(String.valueOf(loRnd.nextInt(10)));
        }

        return lsGCard.toString();
    }

    private String createPinNumber(){
        StringBuilder lsGCard = new StringBuilder();

        Random loRnd = new Random();

        int first = loRnd.nextInt(99);
        int last = loRnd.nextInt(99);

        lsGCard.append(String.format("%02d", loRnd.nextInt(99)));
        lsGCard.append(String.format("%02d", loRnd.nextInt(99)));
        return lsGCard.toString();
    }

    private Date getMembershipDate(String fsGCardNo){
        System.out.println("GCApplication.getMemberShipDate");
        GCard loGCard = new GCard();
        loGCard.setGRider(poGRider);
        loGCard.setWithParent(true);
        UnitGCard loUGCard = (UnitGCard) loGCard.openRecord(fsGCardNo);

        return loUGCard.getMemberSince();
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

    public void setBranch(String fsBranchCD) {
       this.psBranchCD = fsBranchCD;
    }

    public void setWithParent(boolean fbWithParent) {
       this.pbWithParnt = fbWithParent;
    }

    public String getSQ_Master() {
       return (MiscUtil.makeSelect(new UnitGCApplication()));
    }

    public String getCardNo(){
       return psGCardNox;
    }

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
    private String psGCardNox = ""; 

    //cLocation = 0->warehouse; 1->branch; 2->supplier; 3-> customer; 4->unknown 
}
