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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.GCrypt;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GRecord;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.integsys.pojo.UnitMCSerial;

/**
 *
 * @author kalyptus
 */
public class MCSerial implements GRecord{
   private boolean pbWithParnt = false;
   private String psBranchCD = "";
   private String psUserIDxx = "";
   private String psWarnMsg = "";
   private String psErrMsgx = "";
   private GRider poGRider = null;

   public UnitMCSerial newRecord() {
      UnitMCSerial  loOcc = new UnitMCSerial();
      Connection loCon = poGRider.getConnection();

      loOcc.setSerialID(MiscUtil.getNextCode(loOcc.getTable(), "sSerialID", false, loCon, psBranchCD));

      if(!pbWithParnt)
         MiscUtil.close(loCon);

      return loOcc;
   }

   public UnitMCSerial openRecord(String fstransNox) {
      UnitMCSerial  loOcc = new UnitMCSerial();
      Connection loCon = poGRider.getConnection();

      //retrieve the record
      String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sSerialID = " + SQLUtil.toSQL(fstransNox));
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
         Logger.getLogger(MCSerial.class.getName()).log(Level.SEVERE, null, ex);
         setErrMsg(ex.getMessage());
      }
      finally{
         MiscUtil.close(loRS);
         MiscUtil.close(loStmt);
      }

      return loOcc;
   }

   public UnitMCSerial saveRecord(Object foEntity, String fsTransNox) {
      String lsSQL = "";
      UnitMCSerial loOldEnt = null;
      UnitMCSerial loNewEnt = null;
      UnitMCSerial loResult = null;

      // Check for the value of foEntity
      if (!(foEntity instanceof UnitMCSerial)) {
          setErrMsg("Invalid Entity Passed as Parameter");
          return loResult;
      }

      // Typecast the Entity to this object
      loNewEnt = (UnitMCSerial) foEntity;

      //test for the validity of the different fields here
      if (loNewEnt.getEngineNo().equals("")) {
          setMessage("Invalid Engine No Detected");
          return loResult;
      }

      if (loNewEnt.getFrameNo().equals("")) {
          setMessage("Invalid Frame No Detected");
          return loResult;
      }

      //TODO: Test the user rights in these area...

      //Set the value of sModified and dModified here
      GCrypt loCrypt = new GCrypt();

      //Generate the SQL Statement
      if (fsTransNox.equals("")) {
         //TODO: Get new id for this record...
         Connection loCon = poGRider.getConnection();

         loNewEnt.setValue(1, MiscUtil.getNextCode(loNewEnt.getTable(), "sSerialID", false, loCon, psBranchCD ));

         //Generate the INSERT statement
          lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt);
      } else {
          //Reload previous record
          loOldEnt = openRecord(fsTransNox);
          //Generate the UPDATE statement
          lsSQL = MiscUtil.makeSQL((GEntity)loNewEnt, (GEntity)loOldEnt, "sSerialID = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
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

      System.out.println("Inside:" + loResult.getEngineNo());
      return loResult;
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
      return (MiscUtil.makeSelect(new UnitMCSerial()));
   }

   // Added methods here
   public void setGRider(GRider foGRider) {
      this.poGRider = foGRider;
      this.psUserIDxx = foGRider.getUserID();
      if(psBranchCD.isEmpty()) 
         psBranchCD = poGRider.getBranchCode();
   }

    public boolean deleteRecord(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean deactivateRecord(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean activateRecord(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

