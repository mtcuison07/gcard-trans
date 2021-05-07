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
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.GCardStatus;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.GCrypt;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GTransaction;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.StringUtil;
import org.rmj.integsys.pojo.UnitGCard;
import org.rmj.integsys.pojo.UnitGCardRedemption;

/**
 *
 * @author kalyptus
 */
public class GCRedemption implements GTransaction {
   public Object newTransaction() {
      UnitGCardRedemption loOcc = new  UnitGCardRedemption();
      Connection loCon = poGRider.getConnection();

      if(psBranchCD.equals("")) {
         psBranchCD = poGRider.getBranchCode();
      }

      this.pnSupplmnt = (float)0.00;
      this.psORNoxxxx = "";
         
      loOcc.setTransNo(MiscUtil.getNextCode(loOcc.getTable(), "sTransNox", true, loCon, psBranchCD));

      return loOcc;
   }

   public Object loadTransaction(String string) {
      UnitGCardRedemption loOcc = new UnitGCardRedemption();
      Connection loCon = poGRider.getConnection();

      //retrieve the record
      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append(getSQ_Master());
      lsSQL.append(" WHERE sTransNox = ").append(SQLUtil.toSQL(string));

      Statement loStmt = null;
      ResultSet loRS = null;
      try {
         loStmt = loCon.createStatement();
         loRS = loStmt.executeQuery(lsSQL.toString());

         if(!loRS.next())
             setMessage("No transaction Found!");
         else{
            //load each column to the entity
            for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                loOcc.setValue(lnCol, loRS.getObject(lnCol));
            }
         }
      } catch (SQLException ex) {
         Logger.getLogger(GCRedemption.class.getName()).log(Level.SEVERE, null, ex);
         setErrMsg(ex.getMessage());
      }
      finally{
         MiscUtil.close(loRS);
         MiscUtil.close(loStmt);
      }

      return loOcc;
   }

    @Override
    public Object saveUpdate(Object foEntity, String fsTransNox) {
        String lsSQL;
        UnitGCardRedemption loOldEnt = null;
        UnitGCardRedemption loNewEnt = null;
        UnitGCardRedemption loResult = null;

        // Check for the value of foEntity
        if (!(foEntity instanceof UnitGCardRedemption)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return loResult;
        }

        // Typecast the Entity to this object
        loNewEnt = (UnitGCardRedemption) foEntity;

        if(loNewEnt.getGCardNo() == null || loNewEnt.getGCardNo().isEmpty()){
            setErrMsg("No Card detected!");
            return loResult;
        }

        if(loNewEnt.getPromoID() == null || loNewEnt.getPromoID().isEmpty()){
            setErrMsg("No Item was specified!");
            return loResult;
        }

        if(loNewEnt.getTransactDate() == null){
            setErrMsg("Invalid transaction date detected!");
            return loResult;
        }

        if(loNewEnt.getPoints() == null || loNewEnt.getPoints() == 0) {
            setErrMsg("Invalid transaction points detected!");
            return loResult;
        }

        //Check here first if account was already impounded...

        //kalyptus - 2014.09.18 11:52
        //remove 
        loNewEnt.setPoints(loNewEnt.getPoints() - pnSupplmnt);

        loNewEnt.setModifiedBy(psUserIDxx);
        loNewEnt.setDateModified(poGRider.getServerDate());

        //Generate the SQL Statement
        if (fsTransNox.equals("")) {
            //Generate the INSERT statement
            Connection loCon = poGRider.getConnection();

            loNewEnt.setValue(1, MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loCon, psBranchCD));

            lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt);
        } else {
            return null;
        }

        //No changes has been made
        if (lsSQL.equals("")) {
            setMessage("Record is not updated!");
            return loResult;
        }

        if(!pbWithParnt)
           poGRider.beginTrans();

        //kalyptus - 2014.08.06 10:52am
        //issue petron value card here...
        if(loNewEnt.getPromoID().equals("M00114000023")){
            if(!issue_petron_value_card(loNewEnt)){
                if(!pbWithParnt)
                    poGRider.rollbackTrans();
                setMessage("Unable to issue Petron Value Card!");
                return loResult;              
            }
        }
        else if(loNewEnt.getPromoID().equals("M00115000027")){
            System.out.println("Serial:" + psSerialID);
            System.out.println("G Card:" + loNewEnt.getGCardNo());
            System.out.println("Date:" + SQLUtil.dateFormat(loNewEnt.getTransactDate(), "yyyy-MM-dd"));
            /*
             * Kalyptus - 2016.05.30 02:25pm
             *    - FSEP Extension is immediated tagged as active
             */
            String lsService = "INSERT INTO MC_Serial_Service_Extension(sSerialID, sGCardNox, nYellowxx, nWhitexxx, dTransact, cRecdStat)" +
                                 " VALUES( " + SQLUtil.toSQL(psSerialID) +
                                        ", " + SQLUtil.toSQL(loNewEnt.getGCardNo()) +                    
                                        ", " + SQLUtil.toSQL(0) +
                                        ", " + SQLUtil.toSQL(3) +
                                        ", " + SQLUtil.toSQL(SQLUtil.dateFormat(loNewEnt.getTransactDate(), "yyyy-MM-dd")) +
                                        ", " + SQLUtil.toSQL(RecordStatus.ACTIVE) + ")";                  

            poGRider.executeQuery(lsService, "MC_Serial_Service_Extension", "", "");

            if(!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());

                if(!pbWithParnt)
                      poGRider.rollbackTrans();

                return loResult;         
            }
        }

        if(poGRider.executeQuery(lsSQL.toString(), loNewEnt.getTable(), "", "") == 0){
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
                setMessage("No record updated");

            if(!pbWithParnt)
                poGRider.rollbackTrans();
            return loResult;
        }
        else
            loResult = loNewEnt;

        if(poJson != null){
            lsSQL = "INSERT INTO G_Card_Redemption_Digital" +
                   " SET sTransNox = " + SQLUtil.toSQL(loResult.getTransNo()) +
                      ", sIMEINoxx = " + SQLUtil.toSQL((String)poJson.get("sIMEINoxx")) +
                      ", sUserIDxx = " + SQLUtil.toSQL((String)poJson.get("sUserIDxx")) +
                      ", sMobileNo = " + SQLUtil.toSQL((String)poJson.get("sMobileNo")) +
                      ", dQRDateTm = " + SQLUtil.toSQL((String)poJson.get("dQRDateTm"));
            if(poGRider.executeQuery(lsSQL, "G_Card_Redemption_Digital", "", "") == 0){
                if(!poGRider.getErrMsg().isEmpty())
                   setErrMsg(poGRider.getErrMsg());
                else
                   setMessage("Digital info was not saved");
                loResult = null;
            }
        }

        if(loResult != null){
            //kalyptus - 2014.08.18 02:29pm
            //Update inventory information if not a 'PETRON VALUE CARDS' redemption promo
            //MAPFRE insular = M00115000022
            //PETRON 300 = M00114000026
            //PETRON 150 = M00114000025 
            //kalyptus - 2016.01.19 01:14pm
            //Include FSEP = M00115000027
            if(!"M00114000025»M00114000026»M00115000022»M00115000027".contains(loNewEnt.getPromoID())){
                //update of inventory is perform here!
                if(!updateSPInventory(loResult.getPromoID(), loResult.getTransactDate(), true, loResult.getTransNo())){
                    if(!pbWithParnt)
                        poGRider.rollbackTrans();
                    return null;
                }
            }

            //creation of receipt is perform here!
            if(!psORNoxxxx.trim().isEmpty()){
                if(!createOR(psClientID, psORNoxxxx, loResult.getTransactDate(), loResult.getTransNo(), pnSupplmnt)){
                    if(!pbWithParnt)
                        poGRider.rollbackTrans();
                    return null;
                }
            }
        }

        if(!pbWithParnt){
            if(getErrMsg().isEmpty()){
                poGRider.commitTrans();
            }
            else
                poGRider.rollbackTrans();
        }

        return loResult;
    }

   public boolean deleteTransaction(String fsTransNox) {
      UnitGCardRedemption  loOcc = (UnitGCardRedemption) loadTransaction(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      StringBuilder lsSQL = new StringBuilder();
      lsSQL.append("DELETE FROM ").append(loOcc.getTable());
      lsSQL.append(" WHERE sTransNox = ").append(SQLUtil.toSQL(fsTransNox));

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

      //TODO: Write the update to the GCard here
      if(lbResult){

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

   public boolean closeTransaction(String string) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public boolean postTransaction(String string) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public boolean voidTransaction(String fsTransNox) {
      UnitGCardRedemption  loOcc = (UnitGCardRedemption) loadTransaction(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      //TODO: Test the user rights in these area...

      //Create a new detail and then update that detail through the
      //information extracted from the void transaction.
      UnitGCardRedemption loOccNew = (UnitGCardRedemption) newTransaction();
      loOccNew.setCompanyID(loOcc.getCompanyID());
      loOccNew.setGCardNo(loOcc.getGCardNo());
      loOccNew.setSourceCd(loOcc.getSourceCd());
      loOccNew.setSourceNo(loOcc.getSourceNo());
      loOccNew.setPromoID(loOcc.getPromoID());
      loOccNew.setTransactDate(poTransact);
      loOccNew.setPoints(loOcc.getPoints() * -1);
      
      //Set the value of sModified and dModified here
      //GCrypt loCrypt = new GCrypt();
      //loOccNew.setModifiedBy(loCrypt.encrypt(psUserIDxx));
      loOccNew.setModifiedBy(psUserIDxx);
      loOccNew.setDateModified(poGRider.getServerDate());
      
      String lsSQL = MiscUtil.makeSQL((GEntity)loOccNew);
      
      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL, loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record deleted");
      }
      else
         lbResult = true;

      if(!pbWithParnt){
         if(getErrMsg().isEmpty()){
            poGRider.commitTrans();
         }
         else
            poGRider.rollbackTrans();
      }

      return lbResult;
   }

   public boolean cancelTransaction(String fsTransNox) {
      UnitGCardRedemption  loOcc = (UnitGCardRedemption) loadTransaction(fsTransNox);
      boolean lbResult = false;

      if(loOcc == null){
         setMessage("No record found!");
         return lbResult;
      }

      //Create a new detail and then update that detail through the
      //information extracted from the cancelled transaction.
      UnitGCardRedemption loOccNew = (UnitGCardRedemption) newTransaction();
      loOccNew.setCompanyID(loOcc.getCompanyID());
      loOccNew.setGCardNo(loOcc.getGCardNo());
      loOccNew.setTransactDate(poTransact);
      loOccNew.setSourceNo(loOcc.getSourceNo());
      loOccNew.setSourceCd(loOcc.getSourceCd());
      loOccNew.setPromoID(loOcc.getPromoID());
      loOccNew.setPoints(loOcc.getPoints() * -1);

      //Set the value of sModified and dModified here
      //GCrypt loCrypt = new GCrypt();
      //loOccNew.setModifiedBy(loCrypt.encrypt(psUserIDxx));
      loOccNew.setModifiedBy(psUserIDxx);
      loOccNew.setDateModified(poGRider.getServerDate());

      String lsSQL = MiscUtil.makeSQL((GEntity)loOccNew);

      if(!pbWithParnt)
         poGRider.beginTrans();

      if(poGRider.executeQuery(lsSQL, loOcc.getTable(), "", "") == 0){
         if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
         else
            setMessage("No record deleted");
      }
      else
         lbResult = true;

      if(!pbWithParnt){
         if(getErrMsg().isEmpty()){
            poGRider.commitTrans();
         }
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
      return (MiscUtil.makeSelect(new UnitGCardRedemption()));
   }

// add methods here
   public void setGRider(GRider foGRider) {
      this.poGRider = foGRider;
      this.psUserIDxx = foGRider.getUserID();
      if(psBranchCD.isEmpty())
         psBranchCD = poGRider.getBranchCode();
   }

    public String getOTPassword() {
       return psOTPasswd;
    }

    public void setOTPassword(String sOTPasswd) {
       this.psOTPasswd = sOTPasswd;
    }

    public JSONObject getDigital() {
       return poJson;
    }

    public void setDigital(JSONObject foJson) {
       this.poJson = foJson;
    }
   
   public Date getTranDate() {
      return poTransact;
   }

   public void setTranDate(Date date) {
      this.poTransact = date;
   }

   public void setORNo(String fsORNo){
      psORNoxxxx = fsORNo;
   }
   
   public void setCashAmt(float fnCash){
      pnSupplmnt = fnCash;
   }
   
   public void setClient(String fsClientID){
      psClientID = fsClientID;
   }

   public void setSerial(String fsSerialID){
      psSerialID = fsSerialID;
   }
   
   private boolean issue_petron_value_card(UnitGCardRedemption foEntity){
      GCard loGCard = new GCard();
      loGCard.setGRider(poGRider);
      loGCard.setWithParent(true);
      loGCard.setBranch(psBranchCD);
      
      return loGCard.issue_petron_value_card(foEntity.getGCardNo(), foEntity.getTransactDate(), foEntity.getRemarks());
   }
   
   //kalyptus - 2017.09.06 03:51pm
   //replace with Standard SQL Insert
   private boolean updateSPInventory(String sPromoIDx, Date dTransact, boolean bIsNew, String sTransNox){
      StringBuilder lsSQL;
      boolean lbSuccess = true;
      Connection loCon = poGRider.getConnection();
      System.out.println("updateSPInventory");
      
      lsSQL = new StringBuilder();
      lsSQL.append("SELECT sPartsIDx" + 
                        ", nQuantity" +
                  " FROM G_Card_Promo_Detail" + 
                  " WHERE sTransNox = " + SQLUtil.toSQL(sPromoIDx));
      Statement loStmt = null;
      ResultSet loRS = null;

      try {
         loStmt = loCon.createStatement();
         loRS = loStmt.executeQuery(lsSQL.toString());
         System.out.println(lsSQL.toString());
         
         if(!loRS.next()){
             setMessage("No detail found in the promo code!");
             lbSuccess = false;
         }
         else{
            while(!loRS.isAfterLast()){   
                StringBuilder lsSQL1 = new StringBuilder();
                lsSQL1.append("SELECT sPartsIDx" + 
                                   ", nQtyOnHnd" + 
                                   ", nLedgerNo" + 
                             " FROM SP_Inventory" +      
                             " WHERE sPartsIDx = " + SQLUtil.toSQL(loRS.getString("sPartsIDx")) +
                               " AND sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
                Statement loStmt1 = null;
                ResultSet loRS1 = null;
              
                try {
                    loStmt1 = loCon.createStatement();
                    loRS1 = loStmt1.executeQuery(lsSQL1.toString());
                    
                    if(!loRS1.next()) {
                        setMessage("No inventory found for " + loRS.getString("sPartsIDx") + "!");
                        lbSuccess = false;
                    }    
                    else{
                        while(!loRS1.isAfterLast()){
                            StringBuilder lsSQL2 = new StringBuilder();
                            lsSQL2.append("UPDATE SP_Inventory SET" +
                                                "  nQtyOnHnd = nQtyOnHnd" + (bIsNew ? " - " : " + ") + loRS.getInt("nQuantity") +
                                                ", nLedgerNo = " + SQLUtil.toSQL(String.format("%0" + pxeLdgrSize + "d", Integer.parseInt(loRS1.getString("nLedgerNo")) + 1)) + 
                                         " WHERE sPartsIDx = " + SQLUtil.toSQL(loRS1.getString("sPartsIDx")) +
                                           " AND sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
                            poGRider.executeQuery(lsSQL2.toString(), "SP_Inventory", "", "");
                            System.out.println(lsSQL2.toString());
                            
                            StringBuilder lsNme = new StringBuilder();
                            lsSQL2 = new StringBuilder();
                            System.out.println(dTransact);
                            lsNme.append("( sPartsIDx");
                            lsNme.append(", sBranchCd");
                            lsNme.append(", nLedgerNo");
                            lsNme.append(", dTransact");
                            lsNme.append(", sSourceCd");
                            lsNme.append(", sSourceNo");
                            lsNme.append(", nQtyInxxx");
                            lsNme.append(", nQtyOutxx");
                            lsNme.append(", nQtyIssue");
                            lsNme.append(", nQtyOrder");
                            lsNme.append(", nQtyOnHnd");
                            lsNme.append(", nResvOrdr");
                            lsNme.append(", nBackOrdr");
                            lsNme.append(", dModified)");
                            
                            lsSQL2.append("( " + SQLUtil.toSQL(loRS1.getString("sPartsIDx")));
                            lsSQL2.append(", " + SQLUtil.toSQL(poGRider.getBranchCode()));
                            lsSQL2.append(", " + SQLUtil.toSQL(String.format("%0" + pxeLdgrSize + "d", Integer.parseInt(loRS1.getString("nLedgerNo")) + 1)));
                            lsSQL2.append(", " + SQLUtil.toSQL(dTransact));
                            lsSQL2.append(", " + SQLUtil.toSQL(pxeSource));
                            lsSQL2.append(", " + SQLUtil.toSQL(sTransNox));
                            lsSQL2.append(", " + (bIsNew ? 0 : loRS.getInt("nQuantity")));
                            lsSQL2.append(", " + (bIsNew ? loRS.getInt("nQuantity") : 0));
                            lsSQL2.append(", " + 0);
                            lsSQL2.append(", " + 0);
                            lsSQL2.append(", " + (loRS1.getInt("nQtyOnHnd") - (loRS.getInt("nQuantity") * (bIsNew ? +1 : -1))));
                            lsSQL2.append(", " + 0);
                            lsSQL2.append(", " + 0);
                            lsSQL2.append(", " + SQLUtil.toSQL(poGRider.getServerDate(loCon)));
                            lsSQL2.append(")");
                      
                            poGRider.executeQuery("INSERT INTO SP_Inventory_Ledger" + lsNme.toString() + " VALUES " +  lsSQL2.toString(), "SP_Inventory_Ledger", "", "");
                            System.out.println(lsSQL2.toString());
                                            
                            loRS1.next();
                        }
                    }
                } catch (SQLException ex) {
                     Logger.getLogger(GCRedemption.class.getName()).log(Level.SEVERE, null, ex);
                     setErrMsg(ex.getMessage());
                     lbSuccess = false;
                }
                finally{
                    MiscUtil.close(loRS1);
                    MiscUtil.close(loStmt1);
                }
                
                if(!lbSuccess)
                    return lbSuccess;
                else
                    loRS.next(); 
            }
         }
      } catch (SQLException ex) {
         Logger.getLogger(GCRedemption.class.getName()).log(Level.SEVERE, null, ex);
         setErrMsg(ex.getMessage());
         lbSuccess = false;
      }
      finally{
         MiscUtil.close(loRS);
         MiscUtil.close(loStmt);
      }
       
       return lbSuccess;
   }

   //kalyptus - 2017.09.06 03:51pm
   //replace with Standard SQL Insert
   private boolean createOR(String sClientID, String sORNoxxxx, Date dTransact, String sReferNox, float nTranAmtx){
      StringBuilder lsSQL;
      StringBuilder lsNme;
      boolean lbSuccess = true;
      Connection loCon = poGRider.getConnection();
      System.out.println("createOR");
      
      String lsTransNox = MiscUtil.getNextCode("Receipt_Master", "sTransNox", true, loCon, psBranchCD);

      lsNme = new StringBuilder();
      lsSQL = new StringBuilder();
      
      lsNme.append("( sTransNox");
      lsNme.append(", sORNoxxxx");
      lsNme.append(", dTransact");
      lsNme.append(", sSystemCd");
      lsNme.append(", sClientID");
      lsNme.append(", sCoCltID1");
      lsNme.append(", sCoCltID2");
      lsNme.append(", nDiscount");
      lsNme.append(", nOthersxx");
      lsNme.append(", nTranTotl");
      lsNme.append(", sRemarksx");
      lsNme.append(", cTranType");
      lsNme.append(", sAcctNmbr");
      lsNme.append(", sSerialID");
      lsNme.append(", sInsTypID");
      lsNme.append(", cTranStat");
      lsNme.append(", sReferNox");
      lsNme.append(", nTranAmtx");
      lsNme.append(", sSourceCD");
      lsNme.append(", dModified");
      lsNme.append(", sApproved)");

      lsSQL.append("( ").append(SQLUtil.toSQL(lsTransNox));
      lsSQL.append(", ").append(SQLUtil.toSQL(sORNoxxxx));
      lsSQL.append(", ").append(SQLUtil.toSQL(dTransact));
      lsSQL.append(", ").append(SQLUtil.toSQL("GC"));
      lsSQL.append(", ").append(SQLUtil.toSQL(sClientID));
      lsSQL.append(", ").append(SQLUtil.toSQL(""));
      lsSQL.append(", ").append(SQLUtil.toSQL(""));
      lsSQL.append(", ").append(SQLUtil.toSQL(0));
      lsSQL.append(", ").append(SQLUtil.toSQL(0));
      lsSQL.append(", ").append(nTranAmtx);
      lsSQL.append(", ").append(SQLUtil.toSQL("GCard Reward Supplementary Amount"));
      lsSQL.append(", ").append(SQLUtil.toSQL("9"));
      lsSQL.append(", ").append(SQLUtil.toSQL(""));
      lsSQL.append(", ").append(SQLUtil.toSQL(""));
      lsSQL.append(", ").append(SQLUtil.toSQL(""));
      lsSQL.append(", ").append(SQLUtil.toSQL("0"));
      lsSQL.append(", ").append(SQLUtil.toSQL(sReferNox));
      lsSQL.append(", ").append(nTranAmtx);
      lsSQL.append(", ").append(SQLUtil.toSQL(pxeSource));
      lsSQL.append(", ").append(SQLUtil.toSQL(poGRider.getServerDate(loCon)));
      lsSQL.append(", ").append(SQLUtil.toSQL(poGRider.getUserID()));
      lsSQL.append(")");
      
      poGRider.executeQuery("INSERT INTO Receipt_Master" + lsNme.toString() + " VALUES " +  lsSQL.toString(), "Receipt_Master", "", "");
      return lbSuccess;
   }
   
   private boolean pbWithParnt = false;
   private String psBranchCD = "";
   private String psUserIDxx = "";
   private String psWarnMsg = "";
   private String psErrMsgx = "";
   private GRider poGRider = null;
   private Date poTransact = null;

   private String psORNoxxxx;
   private float pnSupplmnt; 
   private String psClientID;
   private String psSerialID;
   private String psOTPasswd = "";
   private JSONObject poJson;
   
   private static String pxeSource = "SPGc"; //GCard Redemption
   private static int pxeLdgrSize = 6;       //nLedgerNo field size 
}
