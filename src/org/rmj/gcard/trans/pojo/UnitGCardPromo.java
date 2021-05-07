/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gcard.trans.pojo;


import java.util.ArrayList;
import org.rmj.integsys.pojo.UnitGCardPromoMaster;
import org.rmj.integsys.pojo.UnitGCardPromoDetail;

/**
 *
 * @author kalyptus
 */
public class UnitGCardPromo {
   UnitGCardPromoMaster master;
   ArrayList <UnitGCardPromoDetail> detail;
//   ArrayList <UnitGCardDetail> online;

   public UnitGCardPromo(){
      master = new UnitGCardPromoMaster();
      detail = new ArrayList<UnitGCardPromoDetail>();
//      online = new ArrayList<UnitGCardDetail>();
   }

   public UnitGCardPromoMaster getMaster(){
      return master;
   }

   public ArrayList<UnitGCardPromoDetail> getDetail(){
      return detail;
   }

   public void setMaster(UnitGCardPromoMaster master){
      this.master = master;
   }

   public void setDetail(ArrayList<UnitGCardPromoDetail> detail){
      if(!this.detail.isEmpty())
         this.detail = new ArrayList<UnitGCardPromoDetail>();
      this.detail.addAll(detail);
   }
}
