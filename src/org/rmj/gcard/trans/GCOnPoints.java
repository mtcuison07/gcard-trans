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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GTransaction;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.gcard.service.GCRestAPI;
import org.rmj.integsys.pojo.UnitGCardDetail;

/**
 *
 * @author kalyptus
 */
public class GCOnPoints implements GTransaction {
    public Object newTransaction() {
        UnitGCardDetail loOcc = new UnitGCardDetail();
        Connection loCon = poGRider.getConnection();

        if(psBranchCD.equals("")) {
           psBranchCD = poGRider.getBranchCode();
        }

        loOcc.setTransNo(MiscUtil.getNextCode(loOcc.getTable(), "sTransNox", true, loCon, psBranchCD));

        return loOcc;
    }

public Object loadTransaction(String fsTransNox) {
    UnitGCardDetail loOcc = new UnitGCardDetail();
    Connection loCon = poGRider.getConnection();

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

    return loOcc;
 }

//make sure that chip will be updated in this area
    @Override
public Object saveUpdate(Object foEntity, String fsTransNox){
    psLastNoxx = "";

    String lsSQL = "";
    UnitGCardDetail loOldEnt = null;
    UnitGCardDetail loNewEnt = null;
    UnitGCardDetail loResult = null;

    // Check for the value of foEntity
    if (!(foEntity instanceof UnitGCardDetail)) {
        setErrMsg("Invalid Entity Passed as Parameter");
        return loResult;
    }

    // Typecast the Entity to this object
    loNewEnt = (UnitGCardDetail) foEntity;

    if(loNewEnt.getGCardNo() == null || loNewEnt.getGCardNo().isEmpty()){
        setErrMsg("No Card detected!");
        return loResult;
    }

    if(loNewEnt.getTransactDate() == null){
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

    if (poGRider.executeUpdate(lsSQL) == 0){
        if(!poGRider.getErrMsg().isEmpty())
            setErrMsg(poGRider.getErrMsg());
        else
            setMessage("No record updated");
    } else loResult = loNewEnt;
                
    try {
        uploadTDS(loResult.getTransNo());
    } catch (SQLException e) {
        e.printStackTrace();
        poGRider.rollbackTrans();
        return loResult;
    }
    
    if(!pbWithParnt){
        if(getErrMsg().isEmpty()){
            psLastNoxx = loResult.getTransNo();
            poGRider.commitTrans();
        } else
            poGRider.rollbackTrans();
    }

    updateMaster(loResult.getGCardNo(), loResult.getPoints(), loResult.getTransNo());

    return loResult;
}

public boolean uploadTDS(String fsTransNox) throws SQLException{
    String lsSQL = "SELECT" +
                        "  sTransNox" +
                        ", sGCardNox" +
                        ", sCompnyID" +
                        ", dTransact" +
                        ", sSourceNo" +
                        ", sSourceCd" +
                        ", nTranAmtx" +
                        ", nPointsxx" +
                        ", sOTPasswd" +
                        ", cPointSnt" +
                    " FROM G_Card_Detail" +
                    " WHERE cPointSnt = '0'" +
                        " AND sTransNox = " + SQLUtil.toSQL(fsTransNox);
    
    ResultSet loRS = poGRider.executeQuery(lsSQL);
    
    if (loRS.next()){        
        String lsTDS = MiscUtil.getNextCode("G_Card_Digital_Transaction", "sTransNox", true, poGRider.getConnection(), psBranchCD);
        
        lsSQL = "INSERT INTO G_Card_Digital_Transaction SET" +
                "  sTransNox = " + SQLUtil.toSQL(lsTDS) +
                ", sGCardNox = " + SQLUtil.toSQL(loRS.getString("sGCardNox")) +
                ", dTransact = " + SQLUtil.toSQL(loRS.getString("dTransact")) +
                ", sBranchCd = " + SQLUtil.toSQL(psBranchCD) +
                ", sSourceNo = " + SQLUtil.toSQL(loRS.getString("sSourceNo")) +
                ", sSourceCd = " + SQLUtil.toSQL(loRS.getString("sSourceCd")) +
                ", sOTPasswd = " + SQLUtil.toSQL(loRS.getString("sOTPasswd")) +
                ", nTranAmtx = " + loRS.getDouble("nTranAmtx") +
                ", nPointsxx = " + loRS.getDouble("nPointsxx") +
                ", cSendStat = '0'" +
                ", cTranStat = '0'" +
                ", sEntryByx = " + SQLUtil.toSQL(poGRider.getUserID()) +
                ", dEntryDte = " + SQLUtil.toSQL(poGRider.getServerDate());
               
        long lnRow = poGRider.executeUpdate(lsSQL);
        
        if (lnRow <= 0){
            setMessage("Unable to create GCard Transaction Digital Signature.");
            return false;
        }
        
        JSONObject response = null;
        
        if (!psBranchCD.equals("M001")){
            response = GCRestAPI.UploadTDS(poGRider, lsTDS);
        } else {
            response = new JSONObject();
            response.put("result", "success");
        }    
        
        poGRider.beginTrans();
        
        String result = (String) response.get("result");
        if(result.equalsIgnoreCase("success")){               
            String sql = "UPDATE G_Card_Detail" +
                        " SET cPointSnt = '1'" + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loRS.getString("sTransNox"));       

            if (poGRider.executeUpdate(sql) == 0){
                poGRider.rollbackTrans();
                setMessage("Unable to update GCard Detail.");
                return false;
            }

            sql = "UPDATE G_Card_Master" + 
                    " SET sLastLine = " + SQLUtil.toSQL(loRS.getString("sTransNox")) + 
                        ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) + 
                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                    " WHERE sGCardNox = " + SQLUtil.toSQL(loRS.getString("sGCardNox"));

            if (poGRider.executeUpdate(sql) == 0){
                poGRider.rollbackTrans();
                setMessage("Unable to update GCard Master.");
                return false;
            }

            sql = "UPDATE G_Card_Digital_Transaction" +
                    " SET cSendStat = '1'" +
                    " WHERE sTransNox = " + SQLUtil.toSQL(lsTDS);

            if (poGRider.executeQuery(sql, "G_Card_Digital_Transaction", "", "") <= 0){
                poGRider.rollbackTrans();
                setMessage("Unable to update GCard TDS.");
                return false; 
            }
        }
        
        poGRider.commitTrans();
    }
    
    return true;
}

public boolean deleteTransaction(String fsTransNox) {
   UnitGCardDetail  loOcc = (UnitGCardDetail) loadTransaction(fsTransNox);
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

    public boolean closeTransaction(String fsTransNox) {
       throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean postTransaction(String fsTransNox) {
       throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean voidTransaction(String fsTransNox) {
        psLastNoxx = "";
        UnitGCardDetail  loOcc = (UnitGCardDetail) loadTransaction(fsTransNox);
        boolean lbResult = true;

        if(loOcc == null){
            setMessage("No record found!");
            return !lbResult;
        }

        //TODO: Test the user rights in these area...
        //Create a new detail and then update that detail through the
        //information extracted from the cancelled transaction.
        UnitGCardDetail loOccNew = (UnitGCardDetail) newTransaction();
        loOccNew.setCompanyID(loOcc.getCompanyID());
        loOccNew.setGCardNo(loOcc.getGCardNo());
        loOccNew.setSourceCd(loOcc.getSourceCd());
        loOccNew.setTransactDate(poTransact);
        loOccNew.setSourceNo(loOcc.getSourceNo());
        loOccNew.setTranAmount(loOcc.getTranAmount() * -1);
        loOccNew.setPoints(loOcc.getPoints() * -1);
        loOccNew.setOTPassword(psOTPasswd);
        //Set the value of sModified and dModified here
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
            lbResult = false;
        }
        else{
            if(poJson != null){
                lsSQL = "INSERT INTO G_Card_Detail_Digital" +
                       " SET sTransNox = " + SQLUtil.toSQL(loOccNew.getTransNo()) +
                          ", sIMEINoxx = " + SQLUtil.toSQL((String)poJson.get("sIMEINoxx")) +
                          ", sUserIDxx = " + SQLUtil.toSQL((String)poJson.get("sUserIDxx")) +
                          ", sMobileNo = " + SQLUtil.toSQL((String)poJson.get("sMobileNo")) +
                          ", dQRDateTm = " + SQLUtil.toSQL((String)poJson.get("dQRDateTm"));
                if(poGRider.executeQuery(lsSQL, "G_Card_Detail_Digital", "", "") == 0){
                    if(!poGRider.getErrMsg().isEmpty())
                       setErrMsg(poGRider.getErrMsg());
                    else
                       setMessage("Digital info was not saved");
                    lbResult = false;
                }
            }
        }

        if(!pbWithParnt){
            if(getErrMsg().isEmpty()){
               psLastNoxx = loOccNew.getTransNo();
               poGRider.commitTrans();
            }
            else
               poGRider.rollbackTrans();
        }

        updateMaster(loOccNew.getGCardNo(), loOccNew.getPoints(), loOccNew.getTransNo());
        
        return lbResult;
    }

    @Override
    public boolean cancelTransaction(String fsTransNox) {
        psLastNoxx = "";
        UnitGCardDetail  loOcc = (UnitGCardDetail) loadTransaction(fsTransNox);
        boolean lbResult = true;

        if(loOcc == null){
            setMessage("No record found!");
            return !lbResult;
        }

        //TODO: Test the user rights in these area...
        //Create a new detail and then update that detail through the
        //information extracted from the cancelled transaction.
        UnitGCardDetail loOccNew = (UnitGCardDetail) newTransaction();
        loOccNew.setCompanyID(loOcc.getCompanyID());
        loOccNew.setGCardNo(loOcc.getGCardNo());
        loOccNew.setSourceCd(loOcc.getSourceCd());
        loOccNew.setTransactDate(poTransact);
        loOccNew.setSourceNo(loOcc.getSourceNo());
        loOccNew.setTranAmount(loOcc.getTranAmount() * -1);
        loOccNew.setPoints(loOcc.getPoints() * -1);
        loOccNew.setOTPassword(psOTPasswd);
        //Set the value of sModified and dModified here
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
            return false;
        }
        else{
            if(poJson != null){
                lsSQL = "INSERT INTO G_Card_Detail_Digital" +
                       " SET sTransNox = " + SQLUtil.toSQL(loOccNew.getTransNo()) +
                          ", sIMEINoxx = " + SQLUtil.toSQL((String)poJson.get("sIMEINoxx")) +
                          ", sUserIDxx = " + SQLUtil.toSQL((String)poJson.get("sUserIDxx")) +
                          ", sMobileNo = " + SQLUtil.toSQL((String)poJson.get("sMobileNo")) +
                          ", dQRDateTm = " + SQLUtil.toSQL((String)poJson.get("dQRDateTm"));
                if(poGRider.executeQuery(lsSQL, "G_Card_Detail_Digital", "", "") == 0){
                    if(!poGRider.getErrMsg().isEmpty())
                       setErrMsg(poGRider.getErrMsg());
                    else
                       setMessage("Digital info was not saved");
                    lbResult = false;
                }
            }
        }

        if(!pbWithParnt){
            if(getErrMsg().isEmpty()){
               psLastNoxx = loOccNew.getTransNo();
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
      return (MiscUtil.makeSelect(new UnitGCardDetail()));
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

    public String getOTPassword() {
       return psOTPasswd;
    }

    public void setOTPassword(String sOTPasswd) {
       this.psOTPasswd = sOTPasswd;
    }

    public String getLastTransNo() {
       return psLastNoxx;
    }
    
    public JSONObject getDigital() {
       return poJson;
    }

    public void setDigital(JSONObject foJson) {
       this.poJson = foJson;
    }

    public ArrayList<UnitGCardDetail> loadLedger(String cardno) {
        UnitGCardDetail loOcc = null;
        Connection loCon = poGRider.getConnection();
        ArrayList<UnitGCardDetail> loArray = new ArrayList<UnitGCardDetail>();

        StringBuilder lsSQL = new StringBuilder();
        lsSQL.append(getSQ_Master());
        lsSQL.append(" WHERE sGCardNox = " + SQLUtil.toSQL(cardno));

        Statement loStmt = null;
        ResultSet loRS = null;
        try {
            loStmt = loCon.createStatement();
            loRS = loStmt.executeQuery(lsSQL.toString());

            while(loRS.next()){
                loOcc = new UnitGCardDetail();
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

   //update Master Record
    private boolean updateMaster(String cardno, Double points, String transno){
        psLastNoxx = transno;
        return true;
    }    

    private boolean pbWithParnt = false;
    private String psBranchCD = "";
    private String psUserIDxx = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private GRider poGRider = null;
    private Date poTransact = null;
    private String psOTPasswd = "";
    private JSONObject poJson;
    private String psLastNoxx="";
}
