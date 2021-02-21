package org.vishia.fblock;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.header2Reflection.CheaderParser;
import org.vishia.header2Reflection.CheaderParser.AttributeOrTypedef;
import org.vishia.header2Reflection.CheaderParser.ZbnfResultData;
import org.vishia.util.Debugutil;

/**This class supports generation of Simulink S-Functions from header files. 
 * 
 * @author hartmut Schorrig
 *
 */
public class SmlkSfn {

  
  /**Version, License and History:
   * <ul>
   * <li>2021-02-19 Hartmut new: Created from jzTc algorithm, Java is more obviously  
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final String version = "2021-02-20";


  public static class ZbnfOpData {
    final CheaderParser.MethodDef zbnfOp;
    
    
    /**Step time designation. 
     * If "!" or "#" then ctor and init. 
     * Else name of the step time as "Tstep", "Tport1" etc.
     * If starts with "+" then it is the update routine. 
     */
    final String steptime;
    
    final Whatisit whatisit;


    public ZbnfOpData(CheaderParser.MethodDef zbnfOp, String steptime, Whatisit whatisit) {
      this.zbnfOp = zbnfOp;
      this.steptime = steptime;
      this.whatisit = whatisit;
    }
    
    
  }
  
  
  
  public static class ZbnfPort {
    final CheaderParser.AttributeOrTypedef zbnfArg;
    final String name;
    final String tstep;
    final String sEnum_SetDefPortTypes;
    final int nr;
    
    public ZbnfPort(AttributeOrTypedef zbnfArg, String name, String tstep, String sEnum_SetDefPortTypes, int nr) {
      this.zbnfArg = zbnfArg;
      this.name = name;
      this.tstep = tstep;
      this.sEnum_SetDefPortTypes = sEnum_SetDefPortTypes;
      this.nr = nr;
    }
    
    @Override public String toString() { return this.name + " @" + this.nr + this.tstep; }

  }
  
  
  
  
  public static class ZbnfFB {
    
    public String name;
    
    public String sBasedOnObject;
    
    public boolean bObject;
    
    public boolean bStatic = true;
    
    /**Type of thiz, remain null for static. */
    public CheaderParser.AttributeOrTypedef thizAttr;
    
    /**A FBlock with only one step time. Simulink: Block bases.
     * The step time may be explicitly by param Tstep or inherit from in/outputs. */
    public boolean isFBstep;
    
    public boolean busInputCheck;  //if any argument _bus, _ybus, _cbus, _ycbus are found, set it to generate check code.
    public boolean busInputGather;
    public boolean busOutputCheck;
    public boolean busOutputGather;
    public boolean bVarg = false;         //set on vaarg in init, step. Then return.ixPworkFBinfo should be >=0 

    public int ixParamTstep = -1;  //index of the Tstep parameter in ctor, -1: not given
    public int ixParamTstep2 = -1; //~same for Tstep2
    public int ixParamStep = 0;    //index of the first parameter for Tstep routine
    public int ixParamUpd = 0;             
    public int ixParamStep2 = 0;   //~
    public int ixParamInit = 0;
    public int ixParamCtor = 0;
    public int ixInputStep = 0;    //index of first input for step An step-in may be before. It is 0 or 1.
    public int ixInputUpd = 0;
    public int ixInputStep2 = 0; 
    public int ixInputInit = 0; 
    public int ixInputThiz = -1; 
    public int ixOutputStep = 0;
    public int ixOutputStep2 = 0;
    public int ixOutputInit = 0;
    public int ixOutputVarg = 0;
    public int ixOutputThizStep = -1;
    public int ixOutputThizInit = -1;
    public int nrofParamsNoTunable = 0;
    public int nrofParamsTunable = 0;
    public int nrofParams = 0;
    public int nrofInputs = 0;                                        
    public int nrofOutputs = 0;
    public int nrofPorts = 0;                      
    public int nrofPortsMax = 64;  //additional possible variable port number
    public StringBuilder paramBitsTunable = new StringBuilder("0");
    public int bitsParamTunable = 0;
    public Map<String, ZbnfPort> paramsNoTunable = new TreeMap<String, ZbnfPort>();   //all names in order of the ix using for ssGetSFcnParam(simstruct, <&ixParams>)

    public int ixDworkThiz = -1;
    public int ixDworkBus = -1;
    public int nrofDwork = 0;
    public int ixPworkFBinfo = -1;
    public int nrofPwork = 0;
    public int ixBusInfo = 0;


    
    
    final List<ZbnfPort> inPorts = new LinkedList<ZbnfPort>();
    
    final Map<String, ZbnfPort> allArgsInIx = new TreeMap<String, ZbnfPort>();
    
    final List<ZbnfPort> outPorts = new LinkedList<ZbnfPort>();
    
    final Map<String, ZbnfPort> allArgsOutIx = new TreeMap<String, ZbnfPort>();
    
    final Map<String, ZbnfPort> allArgsIx = new TreeMap<String, ZbnfPort>();
    
    final List<ZbnfPort> paramPorts = new LinkedList<ZbnfPort>();
    
    final Map<String, ZbnfPort> paramPortIx = new TreeMap<String, ZbnfPort>();
    
    
    ZbnfOpData dataCtor, dataDtor, dataInit, dataTlcParam, dataDPorts, dataUpd, dataOp;
    
    CheaderParser.MethodDef ctor, dtor, init, tlcParam, dPorts, upd, op;
    
    final List<ZbnfOpData> operations = new LinkedList<ZbnfOpData>();

    
    public ZbnfFB() { }
    
    
    /**
     * @param zfb
     * @param zbnfOper if null, does nothing
     */
    public void checkArgs(ZbnfOpData zbnfOper) {
      if(zbnfOper == null) return;
      CheaderParser.MethodDef zbnfOp = zbnfOper.zbnfOp;
      for(CheaderParser.AttributeOrTypedef arg : zbnfOp.args) {
        String name = arg.name;
        if(name.equals("thiz")) {
          if(this.thizAttr == null) { 
            this.thizAttr = arg;
            this.bStatic = false;
            this.sBasedOnObject = arg.type.typeClass().sBasedOnObjectJc;
        } }
        else if(name.equals("othiz")) {
          this.bStatic = false; this.bObject = true;
        }
        else if(name.equals("Tstep")) {
          if(this.paramPortIx.get(name) ==null) {
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, "?", this.nrofParams ++);
            this.paramPorts.add(zbnfPort);
            this.paramPortIx.put(name, zbnfPort);
            this.paramsNoTunable.put(name, zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
            this.ixParamTstep = zbnfPort.nr;
            this.nrofParamsNoTunable +=1;
          }
        }
        else if(name.endsWith("_y")) {
          //name = name.substring(0, name.length()-2);
          if(this.allArgsOutIx.get(name) ==null) {
            String sEnum_DefPortTypes = zbnfOper.whatisit.bInit ? "mOutputInit_Entry_DefPortType_emC" : "mOutputStep_Entry_DefPortType_emC";
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, sEnum_DefPortTypes, this.nrofOutputs ++);
            this.outPorts.add(zbnfPort);
            this.allArgsOutIx.put(name, zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
          }
        }
        else if(name.endsWith("_param")) {
          //name = name.substring(0, name.length()-6);
          if(this.paramPortIx.get(name) ==null) {
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, "?", this.nrofParams ++);
            this.paramPorts.add(zbnfPort);
            this.paramPortIx.put(name, zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
            if(zbnfOper.whatisit.bParamIsTunable && !arg.type.name.equals("StringJc")) {
              this.bitsParamTunable |= 1 << zbnfPort.nr;
              this.nrofParamsTunable +=1;
            } else {
              this.nrofParamsNoTunable +=1;
              this.paramsNoTunable.put(name, zbnfPort);
            }
          }
        }
        else if(zbnfOper.whatisit.bArgIsNonTunableParam) {
          if(this.paramPortIx.get(name) ==null) {
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, "?", this.nrofParams ++);
            this.paramPorts.add(zbnfPort);
            this.paramPortIx.put(name, zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
            this.nrofParamsNoTunable +=1;
            this.paramsNoTunable.put(name, zbnfPort);
          }
        }
        else {
          if(this.allArgsInIx.get(name) ==null) {
            String sEnum_DefPortTypes = zbnfOper.whatisit.bInit ? "mInputInit_Entry_DefPortType_emC" : "mInputStep_Entry_DefPortType_emC";
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, sEnum_DefPortTypes, this.nrofInputs ++);
            this.inPorts.add(zbnfPort);
            this.allArgsInIx.put(name, zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
          }
        }
      }
    }


    public void prepareObjectFB() {
      
      if(this.op.description.simulinkTag.contains("step-in")) {
        String name = "step-in";
        ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", "mStepIn_Entry_DefPortType_emC", this.nrofInputs ++);
        this.inPorts.add(zbnfPort);
        this.allArgsInIx.put(name, zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      if(this.op.description.simulinkTag.contains("step-out")) {
        String name = "step-out";
        ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", "mStepOut_Entry_DefPortType_emC", this.nrofOutputs ++);
        this.outPorts.add(zbnfPort);
        this.allArgsOutIx.put(name, zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      
      
      this.ixInputStep = this.nrofInputs;
      this.ixOutputStep = this.nrofOutputs;
      this.ixParamStep = this.nrofParams;
      checkArgs(this.dataOp);            // build ports & params, firstly from Object-FB
      
      this.ixInputUpd = this.nrofInputs;
      this.ixParamUpd = this.nrofParams;
      checkArgs(this.dataUpd);           // build ports & params, from update
      
      this.ixInputStep2 = this.nrofInputs;
      this.ixOutputStep2 = this.nrofOutputs;
      this.ixParamStep2 = this.nrofParams;
      for(ZbnfOpData zbnfOper : this.operations) {
        checkArgs(zbnfOper);               // build ports & params, from all other operations
      }
      
      this.ixInputInit = this.nrofInputs;
      this.ixOutputInit = this.nrofOutputs;
      this.ixParamInit = this.nrofParams;
      if(this.init !=null) {
        checkArgs(this.dataInit);            // build ports & params, from init
      }
      
      this.ixParamCtor = this.nrofParams;
      if(this.ctor !=null) {
        checkArgs(this.dataCtor);            // build params, from ctor
      }
      this.ixDworkThiz = 0;
      this.nrofDwork = 1;
    
      if(this.ctor == null && !this.bStatic) {
        String name = "thiz";
        ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", "mInputStep_Entry_DefPortType_emC", this.nrofInputs ++);
        this.ixInputThiz = zbnfPort.nr;
        this.inPorts.add(zbnfPort);
        this.allArgsInIx.put(name, zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      if(this.thizAttr == null && this.ctor.type !=null) { // return type of ctor is default the type of FB
        this.thizAttr = new CheaderParser.AttributeOrTypedef("return");
        this.thizAttr.type = this.ctor.type;
        this.thizAttr.name = "return";                         // elsewhere it needs an argument thiz for the type.
      }                                                        // or it is static, without thiz (Operation-FB) 
      
      if(this.ctor != null && !this.op.description.simulinkTag.contains("no-thizStep")) {
        String name = "thizo";
        ZbnfPort zbnfPort = new ZbnfPort(this.thizAttr, name, "Tstep", "mOutputThizStep_Entry_DefPortType_emC", this.nrofOutputs ++);
        this.ixOutputThizStep = zbnfPort.nr;
        this.outPorts.add(zbnfPort);
        this.allArgsOutIx.put(name, zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      if(this.ctor != null && !this.op.description.simulinkTag.contains("no-thizInit")) {
        String name = "ithizo";
        ZbnfPort zbnfPort = new ZbnfPort(this.thizAttr, name, "Tinit", "mOutputThizInit_Entry_DefPortType_emC", this.nrofOutputs ++);
        this.ixOutputThizInit = zbnfPort.nr;
        this.outPorts.add(zbnfPort);
        this.allArgsOutIx.put(name, zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
    
    
    }


    void prepareOperationsFB ( CheaderParser.MethodDef zbnfOp) {
      this.name = zbnfOp.name;
      this.dataOp = new ZbnfOpData(zbnfOp, "Tstep", Whatisit.oper);
      this.op = zbnfOp;
      if(this.name.equals("addObj_DataNode_Inspc"))
        Debugutil.stop();
      checkArgs(this.dataOp);          // build ports & params, firstly from Object-FB
      if(this.thizAttr !=null) {
        String name = "thiz";
        ZbnfPort zbnfPort = new ZbnfPort(this.thizAttr, name, "Tstep", "mInputStep_Entry_DefPortType_emC", this.nrofInputs ++);
        this.inPorts.add(zbnfPort);
        this.allArgsInIx.put(name, zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
        this.ixInputThiz = zbnfPort.nr;
      }
      this.ixInputInit = this.ixInputUpd = this.ixInputStep2 = this.nrofInputs;
      this.ixOutputInit = this.ixOutputStep2 = this.nrofOutputs;
      this.ixParamInit = this.ixParamUpd = this.ixParamStep2 = this.ixParamCtor = this.nrofParams;
      this.isFBstep = true;
      this.ixDworkThiz = -1;
      this.nrofDwork = 0;
      
    }
    
    @Override public String toString() { return this.name; }
  }
  
  
  
  
  enum Whatisit {
    ctor(true, true, false),
    dtor(false, false, false), 
    init(true, false, false),
    oper(false, false, true),
    defTcl(false, false, false),
    defPortTypes(false, false, false);
    final boolean bArgIsNonTunableParam;
    final boolean bParamIsTunable;
    final boolean bInit;
    
    Whatisit( boolean bInit, boolean bArgParam, boolean bParamTunable) {
      this.bInit = bInit;
      this.bArgIsNonTunableParam = bArgParam;
      this.bParamIsTunable = bParamTunable;
    }
  }
  
  
  
  
  public static List<ZbnfFB> analyseOperations(ZbnfResultData parseResult) {
    List<ZbnfFB> fblocks = new LinkedList<ZbnfFB>();
    
    ZbnfFB zfb = new ZbnfFB();
    
    int nStep = 0;
    
    for(CheaderParser.ZbnfResultFile headerfile: parseResult.files){
      for(CheaderParser.ClassC classC: headerfile.listClassC) {
        for(CheaderParser.HeaderBlockEntry entry: classC.entries) {
          if(entry instanceof CheaderParser.StructDefinition) { //  .whatisit == "structDefinition") {
            CheaderParser.StructDefinition zbnfStruct = (CheaderParser.StructDefinition)entry; 
            zfb = new ZbnfFB();                       // new instance ZbnfFB after a struct definition 
            zfb.sBasedOnObject = zbnfStruct.sBasedOnObjectJc;     //String to ObjectJc part
          }
          else if(entry instanceof CheaderParser.MethodDef) {
            CheaderParser.MethodDef zbnfOp = (CheaderParser.MethodDef)entry; 
            if(entry.description !=null && entry.description.simulinkTag !=null) {
              String simulinkTag = entry.description.simulinkTag;
              if(simulinkTag.contains("ctor")) {
                zfb.dataCtor = new ZbnfOpData(zbnfOp, "!", Whatisit.ctor);
                zfb.ctor = zbnfOp;
              }
              else if(simulinkTag.contains("dtor")) {
                zfb.dataDtor = new ZbnfOpData(zbnfOp, "~", Whatisit.dtor);
                zfb.dtor = zbnfOp;
                }
              else if(simulinkTag.contains("init")) {
                zfb.dataInit = new ZbnfOpData(zbnfOp, "Tinit", Whatisit.init);
                zfb.init = zbnfOp;
              } 
              else if(simulinkTag.contains("update")) {
                zfb.dataUpd = new ZbnfOpData(zbnfOp, "+Tstep", Whatisit.oper);
                zfb.upd = zbnfOp;
              } 
              else if(simulinkTag.contains("defTlcParam")) {
                zfb.dataTlcParam = new ZbnfOpData(zbnfOp, "%", Whatisit.defTcl);
                zfb.tlcParam = zbnfOp;
              } 
              else if(simulinkTag.contains("defPortTypes")) {
                zfb.dataDPorts = new ZbnfOpData(zbnfOp, "@", Whatisit.defPortTypes);
                zfb.dPorts = zbnfOp;
              } 
              else if(simulinkTag.contains("PortStep-FB") || simulinkTag.contains("step2")) {
                zfb.operations.add(new ZbnfOpData(zbnfOp, "Tstep" + Integer.toString(++nStep), Whatisit.oper));
              } 
              else if(simulinkTag.contains("Operation-FB")) {
                //                                           // yet a new FB is found as Operation-FB
                ZbnfFB zfbOp = new ZbnfFB();   // write it to a new ZbnfFB
                zfbOp.prepareOperationsFB(zbnfOp);
                fblocks.add(zfbOp);
                //                                           // the current zfb remain active
              } 
              else if(simulinkTag.contains("Object-FB")) {
                zfb.dataOp = new ZbnfOpData(zbnfOp, "Tstep", Whatisit.oper);
                zfb.op = zbnfOp;
                zfb.name = zbnfOp.name;                   // yet a new FB is found as Object-FB
                if(zfb.name.equals("param_PIDf_Ctrl_emC"))
                  Debugutil.stop();
                zfb.prepareObjectFB();
                fblocks.add(zfb);
                ZbnfFB zfbnew = new ZbnfFB();
                zfbnew.sBasedOnObject = zfb.sBasedOnObject;
                zfbnew.dataCtor = zfb.dataCtor;                    // ctor, init etc. remain valid for the next ObjectFB
                zfbnew.dataDtor = zfb.dataDtor;
                zfbnew.dataInit = zfb.dataInit;
                zfbnew.dataDPorts = zfb.dataDPorts;
                zfbnew.dataTlcParam = zfb.dataTlcParam;
                zfbnew.ctor = zfb.ctor;                    // ctor, init etc. remain valid for the next ObjectFB
                zfbnew.dtor = zfb.dtor;
                zfbnew.init = zfb.init;
                zfbnew.dPorts = zfb.dPorts;
                zfbnew.tlcParam = zfb.tlcParam;
                zfb = zfbnew;                              // further usage in new Operation
              } 
          } }
    } } } 
    
    return fblocks;
  }
  
}
