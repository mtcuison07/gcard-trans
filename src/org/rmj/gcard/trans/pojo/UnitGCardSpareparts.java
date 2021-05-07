/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.gcard.trans.pojo;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.rmj.appdriver.iface.GEntity;
//import sun.reflect.LangReflectAccess;

/**
 *
 * @author kalyptus
 */
@Entity
@Table(name = "Spareparts")
public class UnitGCardSpareparts implements Serializable, GEntity {
   private static final long serialVersionUID = 1L;
   @Id
   @Basic(optional = false)
   @Column(name = "sPartsIDx")
   private String sPartsIDx;
   @Column(name = "sBarrcode")
   private String sBarrcode;
   @Column(name = "sDescript")
   private String sDescript;
   @Column(name = "nSelPrice")
   private Double nSelPrice;
   @Column(name = "cPartType")
   private String cPartType;
   @Column(name = "sModified")
   private String sModified;
   @Basic(optional = false)
   @Column(name = "dModified")
   @Temporal(TemporalType.TIMESTAMP)
   private Date dModified;

   public UnitGCardSpareparts() {
      this.sPartsIDx = "";
      this.cPartType = "1";

       //set vector for fields/columns
      laColumns = new LinkedList();
      laColumns.add("sPartsIDx");
      laColumns.add("sBarrcode");
      laColumns.add("sDescript");
      laColumns.add("nSelPrice");
      laColumns.add("cPartType");
      laColumns.add("sModified");
      laColumns.add("dModified");
   }

   public String getPartsID() {
      return sPartsIDx;
   }

   public void setPartsID(String sPartsIDx) {
      this.sPartsIDx = sPartsIDx;
   }

   public String getBarCode() {
      return sBarrcode;
   }

   public void setBarCode(String sBarrCode) {
      this.sBarrcode = sBarrCode;
   }

   public String getDescription() {
      return sDescript;
   }

   public void setDescription(String sDescript) {
      this.sDescript = sDescript;
   }
   
   public Double getSalesPrice() {
      return nSelPrice;
   }

   public void setDescription(Double nSelPrice) {
      this.nSelPrice = nSelPrice;
   }
  
   public String getPartsType() {
      return cPartType;
   }

   public void setPartsType(String cPartType) {
      this.cPartType = cPartType;
   }

   public String getModifiedBy() {
      return sModified;
   }

   public void setModifiedBy(String sModified) {
      this.sModified = sModified;
   }

   public Date getDateModified() {
      return dModified;
   }

   public void setDateModified(Date dModified) {
      this.dModified = dModified;
   }

   @Override
   public int hashCode() {
      int hash = 0;
      hash += (sPartsIDx != null ? sPartsIDx.hashCode() : 0);
      return hash;
   }

   @Override
   public boolean equals(Object object) {
      // TODO: Warning - this method won't work in the case the id fields are not set
      if (!(object instanceof UnitGCardSpareparts)) {
         return false;
      }
      UnitGCardSpareparts other = (UnitGCardSpareparts) object;
      if ((this.sPartsIDx == null && other.sPartsIDx != null) || (this.sPartsIDx != null && !this.sPartsIDx.equals(other.sPartsIDx))) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return "org.rmj.integsys.pojo.UnitBrand[sPartsIDx=" + sPartsIDx + "]";
   }

    public Object getValue(String fsColumn){
       int lnCol = getColumn(fsColumn);
       if(lnCol > 0){
          return getValue(lnCol);
       }
       else
         return null;
    }

   public Object getValue(int fnColumn) {
      switch(fnColumn){
         case 1:
             return this.sPartsIDx;
         case 2:
             return this.sBarrcode;
         case 3:
             return this.sDescript;
         case 4:
             return this.nSelPrice;
         case 5:
             return this.cPartType;
         case 6:
            return this.sModified;
         case 7:
            return this.dModified;
        default:
            return null;
      }
   }

   public String getTable() {
      return "Spareparts";
   }

    public int getColumn(String fsCol) {
        return laColumns.indexOf(fsCol) + 1;
    }

    public String getColumn(int fnCol) {
       if(laColumns.size() < fnCol)
           return "";
       else
           return (String) laColumns.get(fnCol - 1);
    }

    public void setValue(String fsColumn, Object foValue){
       int lnCol = getColumn(fsColumn);
       if(lnCol > 0){
          setValue(lnCol, foValue);
       }
    }

   public void setValue(int fnColumn, Object foValue) {
        switch(fnColumn){
            case 1:
                this.sPartsIDx = (String)foValue;
                break;
            case 2:
                this.sBarrcode = (String)foValue;
                break;
            case 3:
                this.sDescript = (String)foValue;
                break;
            case 4:
                this.nSelPrice = (Double)foValue;
                break;
            case 5:
                this.cPartType = (String)foValue;
                break;
            case 6:
                this.sModified = (String)foValue;
                break;
            case 7:
                this.dModified = (Date)foValue;
                break;
        }
   }

    public int getColumnCount() {
        return laColumns.size();
    }

    @Override
    public void list(){
        Stream.of(laColumns).forEach(System.out::println);        
    }
    
   //Member Variables here
   LinkedList laColumns = null;
}


