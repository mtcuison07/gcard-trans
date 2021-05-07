/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gcard.trans.pojo;

import java.util.ArrayList;
import org.rmj.integsys.pojo.UnitGCard;
import org.rmj.integsys.pojo.UnitGCardDetail;
import org.rmj.integsys.pojo.UnitGCardDetailOffline;

/**
 *
 * @author kalyptus
 */
public class UnitGCardMD {
   UnitGCard master;
   ArrayList <UnitGCardDetailOffline> offline;
//   ArrayList <UnitGCardDetail> online;

   public UnitGCardMD(){
      master = new UnitGCard();
      offline = new ArrayList<UnitGCardDetailOffline>();
//      online = new ArrayList<UnitGCardDetail>();
   }
   
   public UnitGCard getMaster(){
      return master;
   }

   public ArrayList<UnitGCardDetailOffline> getOffline(){
      return offline;
   }

//   public ArrayList<UnitGCardDetail> getOnline(){
//      return online;
//   }

   public void setMaster(UnitGCard master){
      this.master = master;
   }

    public void setOffline(ArrayList<UnitGCardDetailOffline> offline){
        if(this.offline == null)
            this.offline = new ArrayList<UnitGCardDetailOffline>();
        
        if(!this.offline.isEmpty())
            this.offline = new ArrayList<UnitGCardDetailOffline>();
        
        if (offline != null) this.offline.addAll(offline);
    }

//   public void setOnline(ArrayList<UnitGCardDetail> online){
//      if(!this.online.isEmpty())
//         this.online = new ArrayList<UnitGCardDetail>();
//      this.online.addAll(online);
//   }
}
