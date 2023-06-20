import java.util.Random;

public class PipelinedDatapathSimulation {
    public static final short MM_SIZE = 1024; // array called Main_Mem to simulate a 1K Main Memory
    public short[] Main_Mem = new short[MM_SIZE];  // create main memory array
    public static int Regs[] = new int[32];  // create register array

    public int PC = 0x7A000;  // starting address

    public short opCode = 0;

    // create 8 pipeline registers
    IF_ID_Register IF_ID_Read = new IF_ID_Register();
    IF_ID_Register IF_ID_Write = new IF_ID_Register();
    ID_EX_Register ID_EX_Read = new ID_EX_Register();
    ID_EX_Register ID_EX_Write = new ID_EX_Register();
    EX_MEM_Register EX_MEM_Read = new EX_MEM_Register();
    EX_MEM_Register EX_MEM_Write = new EX_MEM_Register();
    MEM_WB_Register MEM_WB_Read = new MEM_WB_Register();
    MEM_WB_Register MEM_WB_Write = new MEM_WB_Register();


    public static void main(String args[]) {
        PipelinedDatapathSimulation me = new PipelinedDatapathSimulation();
        me.initializeMain_Mem();
        me.initializeRegs();
        me.doIt();
    }

    public void doIt() {
        int[] instructions = {0xa1020000, 0x810AFFFC, 0x00831820, 0x01263820, 0x01224820, 0x81180000, 0x81510010,
                0x00624022, 0x00000000, 0x00000000, 0x00000000, 0x00000000};
        //int[] instructions ={0x00a63820, 0x8d0f0004, 0xad09fffc, 0x00625022, 0x10c8fffb, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000};

        // main loop
        for (int i = 0; i < 12; i++) {
            System.out.println("_____________________________________________________________________________________________________");
            int num = i + 1;
            System.out.println("Clock Cycle " + num);

            IF_stage(instructions[i], PC);
            ID_stage();
            EX_stage(); // look what is in the ID_EX_Read == new ID_EX_Register, write data to EX_MEM_Write
            MEM_stage();
            WB_stage();
            Print_out_everything();
            Copy_write_to_read();  // copy contents of variables read to write
            /*System.out.println();
            System.out.println("After we copy the write side of pipeline registers to the read side.");
            Print_out_everything();*/

            PC = IF_ID_Read.getIncrPC();  // increment the PC before it restarts the loop!!
        }
    }
    //Your program will use an array called Main_Mem to simulate a 1K Main Memory.
    // It should be initialized as follows: Main_Mem[0]=0, Main_Mem[1]=1, …Main_Mem[0xFF]=0xFF, Main_Mem[0x100] = 0 and so on.
    // (Note: 0xFF is the largest value that can be put in a byte, so after Main_Mem[0xFF]=0xFF,
    // you start over with Main_Mem[0x100] = 0 and Main_Mem[0x101] = 1.)

    public void initializeMain_Mem() {
        short count = 0;
        while (count < MM_SIZE) {
            for (short i = 0; i < 0xFF + 1; i++) {
                Main_Mem[count] = i;
                //System.out.printf(" %x ", Main_Mem[count]);
                count++;
            }
        }
    }

    //These registers are given initial values of x100 plus the register number except for register 0 which always has the value 0.
    // (So $0=0, $1=0x101, $2=0x102, ... $10 = 0x10a, … $31=0x11f. register numbers are decimal.
    public void initializeRegs() {
        for (short i = 0; i < 32; i++) {
            if (i == 0) {
                Regs[i] = 0;  // register 0 which always has the value 0
            } else {
                Regs[i] = 0x100 + i;
            }
        }
    }

    public static String convertGarbageVal(short val){
        if(val == 4095 || val == 0xfff){
            return "X";
        }
        return Integer.toString(val);
    }
//    public static String convertGarbageValtoHex(short val){
//        if(val == 4095 || val == 0xfff){
//            return "X";
//        }
//        return Integer.toHexString(val);
//    }

    public static String convertOffsetVal(short val){
        if(val == 4095 || val == 0xfff){
            return "X";
        }else {
            return String.format("%8s", Integer.toHexString(val)).replace(" ", "0");
        }
        //return Integer.toString(val);
    }
    public void Print_out_everything() {
        // IF/ID Write and Read
        System.out.println("IF/ID Write(written to by the IF stage)");
        if (IF_ID_Write.getInstruction() == 0) {
            System.out.println("Inst = 0x00000000");
        } else {
            System.out.printf("Inst = 0x%X            ", IF_ID_Write.getInstruction());
            System.out.printf("IncrPC = %X ", IF_ID_Write.getIncrPC());
            System.out.println();
        }
        System.out.println();
        System.out.println("IF/ID Read(read by the ID stage)");
        if (IF_ID_Read.getInstruction() == 0) {
            System.out.println("Control = 000000000");
        } else {
            System.out.printf("Inst = 0x%X            ", IF_ID_Read.getInstruction());
            System.out.printf("IncrPC = %X ", IF_ID_Read.getIncrPC());
            System.out.println();
        }
        System.out.println();
        System.out.println("____________________________________");

        // ID/EX write and read
        System.out.println("ID/EX Write(written to by the ID stage)");
        if (ID_EX_Write.getIncrPC() == 0) {  // use IncrePC value to check if there is data in this pipeline register
            System.out.println("Control = 000000000");
        } else {
            System.out.printf("Control: RegDst = %s,      ALUSrc = %X,     ALUOp = 0b" + String.format("%2s", Integer.toBinaryString(ID_EX_Write.getALUOp()).replace(' ', '0')) + ",",
                    convertGarbageVal(ID_EX_Write.getRegDst()), ID_EX_Write.getALUSrc());
            System.out.printf("     MemRead = %X,        MemWrite = %X,      Branch = %X,        MemToReg = %s,       RegWrite = %X", ID_EX_Write.getMemRead(), ID_EX_Write.getMemWrite(),
                    ID_EX_Write.getBranch(), convertGarbageVal(ID_EX_Write.getMemToReg()),ID_EX_Write.getRegWrite());
            System.out.println();
            System.out.printf("IncrPC = %X,    ReadReg1Value = %X,    ReadReg2Value = %X", ID_EX_Write.getIncrPC(), ID_EX_Write.getReadData1(),ID_EX_Write.getReadData2());
            System.out.println();
            System.out.printf("SEOffset = " + convertOffsetVal(ID_EX_Write.getSEOffset()) + ",     WriteReg_20_16 = " + ID_EX_Write.getWreg_20_16()
                    + ",    WriteReg_15_11 = " + ID_EX_Write.getWreg_15_11()+ ",    Function = %s\n", convertGarbageVal(ID_EX_Write.getFuncCode()));
        }
        System.out.println();
        System.out.println("ID/EX Read (read by the EX stage)");
        if (ID_EX_Read.getIncrPC() == 0) {  // use IncrePC value to check if there is data in this pipeline register
            System.out.println("Control = 000000000");
        } else { // garbage value?
            System.out.printf("Control: RegDst = %s,      ALUSrc = %X,     ALUOp = 0b" + String.format("%2s", Integer.toBinaryString(ID_EX_Read.getALUOp()).replace(' ', '0')) + ",",
                    convertGarbageVal(ID_EX_Read.getRegDst()), ID_EX_Read.getALUSrc());
            System.out.printf("     MemRead = %X,        MemWrite = %X,      Branch = %X,        MemToReg = %s,       RegWrite = %X", ID_EX_Read.getMemRead(), ID_EX_Read.getMemWrite(),
                    ID_EX_Read.getBranch(), convertGarbageVal(ID_EX_Read.getMemToReg()), ID_EX_Read.getRegWrite());
            System.out.println();
            System.out.printf("IncrPC = %X,     ReadReg1Value = %X,     ReadReg2Value = %X", ID_EX_Read.getIncrPC(), ID_EX_Read.getReadData1(), ID_EX_Read.getReadData2());
            System.out.println();
            System.out.printf("SEOffset = " + convertOffsetVal(ID_EX_Read.getSEOffset()) + ",    WriteReg_20_16 = " + ID_EX_Read.getWreg_20_16()
                    + ",    WriteReg_15_11 = " + ID_EX_Read.getWreg_15_11()+ ",    Function = %s\n",  convertGarbageVal(ID_EX_Read.getFuncCode()));
        }
        System.out.println();
        System.out.println("____________________________________");

        // EX/MEM write and read
        System.out.println("EX/MEM Write(written to by the EX stage)");
        //if (EX_MEM_Write.getALUResult() == 0) { // use ALUResult value to tell if there is data in this pipeline register
         //   System.out.println("Control = 000000000");
       // } else { // garbage value?
            System.out.println("Control: MemRead = " + EX_MEM_Write.getMemRead() + ", MemWrite = " + EX_MEM_Write.getMemWrite()
                    + ", Branch = " + EX_MEM_Write.getBranch() + ", MemToReg = " + convertGarbageVal(EX_MEM_Write.getMemToReg()) + ", RegWrite = " + EX_MEM_Write.getRegWrite());

            System.out.printf("ALUResult = %X,      SWValue = %X,        WriteRegNum = %s\n",
                    EX_MEM_Write.getALUResult(), EX_MEM_Write.getSwValue(), convertGarbageVal(EX_MEM_Write.getWriteRegNum()));
       // }
        System.out.println();
        System.out.println("EX/MEM Read (read by the MEM stage)");
       // if (EX_MEM_Read.getALUResult() == 0) { // use ALUResult value to tell if there is data in this pipeline register
          //  System.out.println("Control = 000000000");
       // } else { // garbage value?
            System.out.println("Control: MemRead = " + EX_MEM_Read.getMemRead() + ", MemWrite = " + EX_MEM_Read.getMemWrite()
                    + ", Branch = " + EX_MEM_Read.getBranch() + ", MemToReg = " + convertGarbageVal(EX_MEM_Read.getMemToReg()) + ", RegWrite = " + EX_MEM_Read.getRegWrite());

            System.out.printf("ALUResult = %X,      SWValue = %X,        WriteRegNum = %s\n",
                    EX_MEM_Read.getALUResult(), EX_MEM_Read.getSwValue(), convertGarbageVal(EX_MEM_Read.getWriteRegNum()));
       // }
        System.out.println();
        System.out.println("____________________________________");

        // MEM/WB write and read
        System.out.println("MEM/WB Write(written to by the MEM stage)");
       // if (MEM_WB_Write.getALUResult() == 0) { // use ALUResult value to tell if there is data in this pipeline register
           // System.out.println("Control = 000000000");
       // } else { //
            System.out.println("Control: MemToReg = " + convertGarbageVal(MEM_WB_Write.getMemToReg()) + ", RegWrite = " + MEM_WB_Write.getRegWrite());

            System.out.printf("LWDataValue = %s,           ALUResult = %x,         WriteRegNum = %s\n",
                    convertGarbageVal(MEM_WB_Write.getLWDataValue()), MEM_WB_Write.getALUResult(), convertGarbageVal(MEM_WB_Write.getWriteRegNum()));
      //  }

        System.out.println();
        System.out.println("MEM/WB Read(read by the WB stage)");
       // if (MEM_WB_Read.getALUResult() == 0) { // use ALUResult value to tell if there is data in this pipeline register
           // System.out.println("Control = 000000000");
       // } else { //
            System.out.println("Control: MemToReg = " + convertGarbageVal(MEM_WB_Read.getMemToReg()) + ", RegWrite = " + MEM_WB_Read.getRegWrite());

            System.out.printf("LWDataValue = %s,           ALUResult = %x,         WriteRegNum = %s\n",
                    convertGarbageVal(MEM_WB_Read.getLWDataValue()), MEM_WB_Read.getALUResult(), convertGarbageVal(MEM_WB_Read.getWriteRegNum()));
        //}

        System.out.println("____________________________________");
        System.out.println();

        // print register array
        System.out.println("Registers:");
        int i = 0;
        for(int regNum : Regs){
            System.out.printf("$%d = 0x%x   ", i, regNum);
            i++;
        }
        System.out.println("\n\n");

    }

    public void Copy_write_to_read() { // !!deep copy: copy every variable!!
        IF_ID_Read.incrPC = IF_ID_Write.incrPC;
        IF_ID_Read.instruction = IF_ID_Write.instruction;

        ID_EX_Read.RegDst = ID_EX_Write.RegDst;
        ID_EX_Read.ALUSrc = ID_EX_Write.ALUSrc;
        ID_EX_Read.ALUOp = ID_EX_Write.ALUOp;
        ID_EX_Read.MemRead = ID_EX_Write.MemRead;
        ID_EX_Read.MemWrite = ID_EX_Write.MemWrite;
        ID_EX_Read.Branch = ID_EX_Write.Branch;
        ID_EX_Read.MemToReg = ID_EX_Write.MemToReg;
        ID_EX_Read.RegWrite = ID_EX_Write.RegWrite;
        ID_EX_Read.IncrPC = ID_EX_Write.IncrPC;
        ID_EX_Read.ReadData1 = ID_EX_Write.ReadData1;
        ID_EX_Read.ReadData2 = ID_EX_Write.ReadData2;
        ID_EX_Read.SEOffset = ID_EX_Write.SEOffset;
        ID_EX_Read.Wreg_20_16 = ID_EX_Write.Wreg_20_16;
        ID_EX_Read.Wreg_15_11 = ID_EX_Write.Wreg_15_11;
        ID_EX_Read.FuncCode = ID_EX_Write.FuncCode;

        EX_MEM_Read.MemRead = EX_MEM_Write.MemRead;
        EX_MEM_Read.MemWrite = EX_MEM_Write.MemWrite;
        EX_MEM_Read.Branch = EX_MEM_Write.Branch;
        EX_MEM_Read.MemToReg = EX_MEM_Write.MemToReg;
        EX_MEM_Read.RegWrite = EX_MEM_Write.RegWrite;
        EX_MEM_Read.ALUResult = EX_MEM_Write.ALUResult;
        EX_MEM_Read.SwValue = EX_MEM_Write.SwValue;
        EX_MEM_Read.WriteRegNum = EX_MEM_Write.WriteRegNum;

        MEM_WB_Read.MemToReg = MEM_WB_Write.MemToReg;
        MEM_WB_Read.RegWrite = MEM_WB_Write.RegWrite;
        MEM_WB_Read.LWDataValue = MEM_WB_Write.LWDataValue;
        MEM_WB_Read.ALUResult = MEM_WB_Write.ALUResult;
        MEM_WB_Read.WriteRegNum = MEM_WB_Write.WriteRegNum;

    }

    public void IF_stage(int instruction, int PC) {
        // write the value in the pipeline register: IF_ID_Write
        IF_ID_Write.setInstruction(instruction);
        IF_ID_Write.setIncrPC(PC + 4);
        // IF_ID_Write.incrPC = PC + 4;

    }

    public void ID_stage() {
        // read instruction from IF_ID_Read and decode instruction
        // not decide whether it is i-format or r-format at ID_stage
        opCode = (short) ((BitMask.bitMask31_26(IF_ID_Read.getInstruction())) >>> 26);

//        ID_EX_Write.setSEOffset((short) BitMask.bitMask15_0(IF_ID_Read.getInstruction()));    // ID stage doesn't decide the i-format or r-format
//        ID_EX_Write.setWreg_20_16((short) ((BitMask.bitMask20_16(IF_ID_Read.getInstruction())) >>> 16));
//        ID_EX_Write.setWreg_15_11((short) ((BitMask.bitMask15_11(IF_ID_Read.getInstruction())) >>> 11));   // destination registration


        // set the control bits
        // if opCode == 0, it is R-format
        if (opCode == 0) {
            // extract funCode, no need to shift
            ID_EX_Write.setFuncCode ((short) (BitMask.bitMask5_0(IF_ID_Read.getInstruction())));

            short firstSourceReg = (short) ((BitMask.bitMask25_21(IF_ID_Read.getInstruction())) >>> 21);   // first source registration
            short secondSourceReg = (short) ((BitMask.bitMask20_16(IF_ID_Read.getInstruction())) >>> 16); // second source registration
            short destReg = (short) ((BitMask.bitMask15_11(IF_ID_Read.getInstruction())) >>> 11);  // destination register


            if (ID_EX_Write.getFuncCode() == 0x20 || ID_EX_Write.getFuncCode() == 0x22) { // 0x20 is add, 0x22 is subtract
                    ID_EX_Write.setRegDst((short) 0b1);
                    ID_EX_Write.setALUSrc((short) 0b0);
                    ID_EX_Write.setALUOp((short) 0b10); // binary literal
                    ID_EX_Write.setMemRead((short) 0b0);
                    ID_EX_Write.setMemWrite((short) 0b0);
                    ID_EX_Write.setBranch((short) 0b0);
                    ID_EX_Write.setMemToReg((short) 0b0);
                    ID_EX_Write.setRegWrite((short) 0b1);

                    ID_EX_Write.setReadData1((short) Regs[firstSourceReg]);   // fetch the value from Registration array get the value "0x10..." at the address of sifrst Source Reg
                    ID_EX_Write.setReadData2((short) Regs[secondSourceReg]);  // second source registration

                    ID_EX_Write.setWreg_20_16(secondSourceReg);
                    ID_EX_Write.setWreg_15_11(destReg);

                    ID_EX_Write.setSEOffset((short)0xfff); // SEOffset is garbage value in R-format

//                case 0x24:   // and
//                    ID_EX_Write.setRegDst((short) 0b1);
//                    ID_EX_Write.setALUSrc((short) 0b0);
//                    ID_EX_Write.setALUOp((short) 0b000); // binary literal
//                    ID_EX_Write.setMemRead((short) 0b0);
//                    ID_EX_Write.setMemWrite((short) 0b0);
//                    ID_EX_Write.setBranch((short) 0b0);
//                    ID_EX_Write.setMemToReg((short) 0b0);
//                    ID_EX_Write.setRegWrite((short) 0b1);
//                    break;
//
//                case 0x25:   // or
//                    ID_EX_Write.setRegDst((short) 0b1);
//                    ID_EX_Write.setALUSrc((short) 0b0);
//                    ID_EX_Write.setALUOp((short) 0b001); // binary literal
//                    ID_EX_Write.setMemRead((short) 0b0);
//                    ID_EX_Write.setMemWrite((short) 0b0);
//                    ID_EX_Write.setBranch((short) 0b0);
//                    ID_EX_Write.setMemToReg((short) 0b0);
//                    ID_EX_Write.setRegWrite((short) 0b1);
//                    break;

            }

        } else {// if opCode != 0, it is I-format
            //ID_EX_Write.setFuncCode((short)0xfff); // func code is garbage value in I-format

            short rs = (short) ((BitMask.bitMask25_21(IF_ID_Read.getInstruction())) >>> 21);   // first source registration
            short rt = (short) ((BitMask.bitMask20_16(IF_ID_Read.getInstruction())) >>> 16); // second source registration
           // ID_EX_Write.setSEOffset((short) ((BitMask.bitMask15_0(IF_ID_Read.getInstruction()))));    // ID stage doesn't decide the i-format or r-format

          //  ID_EX_Write.setReadData1((short) Regs[firstSourceReg]);   // fetch the value from Registration array get the value "0x10..." at the address of sifrst Source Reg
          //  ID_EX_Write.setReadData2((short) Regs[destReg]);  // destination registration

         //   ID_EX_Write.setWreg_20_16(destReg);
         //   ID_EX_Write.setWreg_15_11((short)((BitMask.bitMask15_11((IF_ID_Read.getInstruction()))) >>> 11));   // destination registration

            switch (opCode) {
                case 0x20:   // lb
                    ID_EX_Write.setRegDst((short) 0b0);
                    ID_EX_Write.setALUSrc((short) 0b1);// load is going to use an offset for the  lower input into the ALU
                    ID_EX_Write.setALUOp((short) 0b00); // It tells ALU to do an add, register value + offset ???
                    ID_EX_Write.setMemRead((short) 0b1); // load will read from MM
                    ID_EX_Write.setMemWrite((short) 0b0);
                    ID_EX_Write.setBranch((short) 0b0);
                    ID_EX_Write.setMemToReg((short) 0b1);
                    ID_EX_Write.setRegWrite((short) 0b1);  // it will write value to the register

                    ID_EX_Write.setSEOffset((short) ((BitMask.bitMask15_0(IF_ID_Read.getInstruction()))));
                    ID_EX_Write.setFuncCode((short)0xfff); // func code is garbage value in I-format

                    ID_EX_Write.setReadData1((short) Regs[rs]);   // fetch the value from Registration array get the value "0x10..."
                    ID_EX_Write.setReadData2((short) Regs[rt]);  // register address we want to write the value to

                    ID_EX_Write.setWreg_20_16(rt);
                    ID_EX_Write.setWreg_15_11((short)((BitMask.bitMask15_11((IF_ID_Read.getInstruction()))) >>> 11));
                    break;

                case 0x28:     // sb
                    ID_EX_Write.setRegDst((short)0xfff);  //gibberish value？？？？ it will be ignored in the later stage
                    ID_EX_Write.setALUSrc((short) 0b1);
                    ID_EX_Write.setALUOp((short) 0b00); // binary literal
                    ID_EX_Write.setMemRead((short) 0b0);
                    ID_EX_Write.setMemWrite((short) 0b1);
                    ID_EX_Write.setBranch((short) 0b0);
                    ID_EX_Write.setMemToReg((short)0xfff); //gibberish value？？？？ it will be ignored in the later stage
                    ID_EX_Write.setRegWrite((short) 0b0);

                    ID_EX_Write.setSEOffset((short) ((BitMask.bitMask15_0(IF_ID_Read.getInstruction()))));
                    ID_EX_Write.setFuncCode((short)0xfff); // func code is garbage value in I-format

                    ID_EX_Write.setReadData1((short) Regs[rs]);   // fetch the value from Registration array get the value "0x10..." at the address of sifrst Source Reg
                    ID_EX_Write.setReadData2((short) Regs[rt]);  // data that will be written to MainMem

                    ID_EX_Write.setWreg_20_16(rt);
                    ID_EX_Write.setWreg_15_11((short)((BitMask.bitMask15_11((IF_ID_Read.getInstruction()))) >>> 11));

                    break;
            }

        }

        // write the values to ID_EX_Write
        ID_EX_Write.setIncrPC(IF_ID_Read.getIncrPC());
    }

    public void EX_stage() {
        // pass the five control signal from ID_EX_Read to EX_MEM_Write
        EX_MEM_Write.setMemRead(ID_EX_Read.getMemRead());
        EX_MEM_Write.setMemWrite(ID_EX_Read.getMemWrite());
        EX_MEM_Write.setBranch(ID_EX_Read.getBranch());
        EX_MEM_Write.setMemToReg(ID_EX_Read.getMemToReg());
        EX_MEM_Write.setRegWrite(ID_EX_Read.getRegWrite());

        // read RegDst signal from ID_EX_Read is 1(R-format), The address to write to in Register is from WriteReg_15_11,
        // so write that number to the WriteRegNum in  EX_MEM_Write
        // if read RegDst signal 0 (i-format), The address to write to in Register is from WriteReg_20_16,
        // so write that number to the WriteRegNum in  EX_MEM_Write
        // if read RegDst signal garbage value, it doesn't matter which num writes to WriteRegNum, just randomly pick 1.
        if (ID_EX_Read.getRegDst() == 1) {  // if RegDest is 1, R format, it will write to register at address, Wreg_15_11
            EX_MEM_Write.setWriteRegNum(ID_EX_Read.getWreg_15_11());
        } else if (ID_EX_Read.getRegDst() == 0) {   // If RegDest is 0, it is load, it will write to register at address, Wreg_20_16
            EX_MEM_Write.setWriteRegNum(ID_EX_Read.getWreg_20_16());
        } else { // store  ??don't care the write register number or garbage value??
            Random rand = new Random();//just do a coin flip from Wreg_15_11 or Wreg_20_16
            if (rand.nextBoolean()) {
                EX_MEM_Write.setWriteRegNum(ID_EX_Read.getWreg_15_11());
            } else {
                EX_MEM_Write.setWriteRegNum(ID_EX_Read.getWreg_20_16());
            }
            //EX_MEM_Write.setWriteRegNum((short)0xfff); // If it is store, WriteRegNum is garbage value
        }


        // read ALUSrc signal from ID_EX_Read, if it is 0 (R-format), will take upper input to ALU to perform operation,
        // then write the result to ALUresult in EX_MEM_Write
        // if ALUSrc signal is 1, will take lower input to ALU to perform load or store, then add readdata1 to SEOffset,
        // then write the result to ALUrestul in EX_MEM_Write.
        if (ID_EX_Read.getALUSrc() == 1) {// load or store, then we'll add the SEOffset to ReadData1
            if(opCode == 0x20){// load byte
                EX_MEM_Write.setALUResult((short) (ID_EX_Read.getReadData1() + ID_EX_Read.getSEOffset()));
                EX_MEM_Write.setSwValue(ID_EX_Read.getReadData2());

            }else if(opCode == 0x28){// store word
                EX_MEM_Write.setALUResult((short) (ID_EX_Read.getReadData1() + ID_EX_Read.getSEOffset()));
                EX_MEM_Write.setSwValue(ID_EX_Read.getReadData2());
            }

        } else if (ID_EX_Read.getALUSrc() == 0){  //if ALUsource is 0, it is R-format, we'll decide the operation between Read data 1 and Read data 2
            if (ID_EX_Read.getFuncCode() == 0x20) {
                EX_MEM_Write.setALUResult((short) (ID_EX_Read.getReadData1() + ID_EX_Read.getReadData2()));
                EX_MEM_Write.setSwValue(ID_EX_Read.getReadData2());

            } else if (ID_EX_Read.getFuncCode() == 0x22) { // subtract
                EX_MEM_Write.setALUResult((short) (ID_EX_Read.getReadData1() - ID_EX_Read.getReadData2()));
                EX_MEM_Write.setSwValue(ID_EX_Read.getReadData2());

            }
        }
        // Remember to read ReadData2 and write to StoreWordValue
       // EX_MEM_Write.setSwValue(ID_EX_Read.getReadData2());

    }

    public void MEM_stage() {
        // pass the left 2 control signal from EX_MEM_Read to MEM_WB_Write
        MEM_WB_Write.setMemToReg(EX_MEM_Read.getMemToReg());
        MEM_WB_Write.setRegWrite(EX_MEM_Read.getRegWrite());

        //Read value from EX_MEM Read
        //If MemRead=1, it is lb, then use ALUResult (the address you calculated in the EX stage) as an index into your Main Memory array
        //and get the value there, write it to LWDatavalue in MEM_WB_Write
        //If MemWrite=1, it is sb, then store the value (SwValue from EX_MEM_Read) to MM array at the address of ALUResult
        if (EX_MEM_Read.getMemRead() == 1) { //lb
            MEM_WB_Write.setLWDataValue(Main_Mem[EX_MEM_Read.getALUResult()]);
            MEM_WB_Write.setALUResult(EX_MEM_Read.getALUResult());
            MEM_WB_Write.setWriteRegNum(EX_MEM_Read.getWriteRegNum());

        } else if (EX_MEM_Read.getMemWrite() == 1) { // sb
            Main_Mem[EX_MEM_Read.getALUResult()] = EX_MEM_Read.getSwValue();
            MEM_WB_Write.setLWDataValue((short)0xfff);
            MEM_WB_Write.setALUResult(EX_MEM_Read.getALUResult());
            MEM_WB_Write.setWriteRegNum((short)0xfff);  // garbage value

        }else if (EX_MEM_Read.getMemWrite() == 0 && EX_MEM_Read.getMemRead() == 0){
            //for r-format instruction, just pass along data from EX_MEM_Read to MEM_WB_Write
            MEM_WB_Write.setALUResult(EX_MEM_Read.getALUResult());
            MEM_WB_Write.setWriteRegNum(EX_MEM_Read.getWriteRegNum());
            MEM_WB_Write.setLWDataValue((short)0xfff);
        }

    }

    public void WB_stage() {
        //Write to the registers based on information you read out of the READ version of MEM_WB
        if(MEM_WB_Read.getRegWrite() == 1) {
            if (MEM_WB_Read.getMemToReg() == 0) { // R type, write the result to register at the address of WriteRegNum
                Regs[MEM_WB_Read.getWriteRegNum()] = MEM_WB_Read.getALUResult();
                //MEM_WB_Write.setLWDataValue((short) 0xfff);
            } else if (MEM_WB_Read.getMemToReg() == 1) {  // (MemtoReg == 1, Load byte, write the result loaded from MM to register  at the address of WriteRegNum
                MEM_WB_Read.setLWDataValue((Main_Mem)[MEM_WB_Read.getALUResult()]);
                Regs[MEM_WB_Read.getWriteRegNum()] = MEM_WB_Read.getLWDataValue();
               // MEM_WB_Write.setALUResult((short) 0xfff);
            }
        }

    }


}

    /*public short twoComplement(short val) {
        if(val >>> 15 == 1){
            val = (short)(val - 2 ^ 16);
        }
        return val;
    }*/
