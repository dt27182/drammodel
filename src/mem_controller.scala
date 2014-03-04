package DRAMModel

import Chisel._;
import Node._;
import DRAMModel.MemModelConstants._

class MemData extends Bundle {
  val data = Bits(width = MEM_DATA_BITS)
}

class MemReqCmd() extends Bundle 
{
  val rw = Bool()
  val addr = UInt(width = PADDR_BITS - OFFSET_BITS)
  val tag = Bits(width = MEM_TAG_BITS)
}

class MemResp () extends MemData
{
  val tag = Bits(width = MEM_TAG_BITS)
}

class ioMem() extends Bundle
{
  val req_cmd  = new DecoupledIO(new MemReqCmd() )
  val req_data = new DecoupledIO( new MemData() )
  val resp     = new DecoupledIO( new MemResp() ).flip
}


class MemController() extends Module {
  val io= new Bundle {
    val mem_cmd_queue = new DecoupledIO(new MemReqCmd()).flip()
    val mem_data_queue = new DecoupledIO(new MemData()).flip()
    val mem_resp_queue = new DecoupledIO(new MemResp())
    val DRAMModel = new DRAMModelToMemControllerIO().flip()
    val fireTgtCycle = Bool(INPUT)
    val params = new BankParameterIO()
    //val debug_out = Vec.fill(DEVICE_WIDTH){new MemData}.asOutput
    //val debug_readData = Bits().asOutput
  }
  val timer = Module(new MemControllerTimer())
  timer.io.params <> io.params
  timer.io.fireTgtCycle := io.fireTgtCycle
  timer.io.cmds.valid := io.DRAMModel.cmdBus.valid
  timer.io.cmds.activate := io.DRAMModel.cmdBus.cmd === activate_cmd
  timer.io.cmds.read := io.DRAMModel.cmdBus.cmd === read_cmd
  timer.io.cmds.write := io.DRAMModel.cmdBus.cmd === write_cmd
  timer.io.cmds.precharge := io.DRAMModel.cmdBus.cmd === precharge_cmd
  timer.io.cmds.bankAddr := io.DRAMModel.cmdBus.bankAddr
  timer.io.cmds.rowAddr := io.DRAMModel.cmdBus.rowAddr
  timer.io.cmds.colAddr := io.DRAMModel.cmdBus.colAddr
  
  val currentAddr = Reg(init = UInt(0, PADDR_BITS - OFFSET_BITS))
  val nextAddr = UInt(); nextAddr := currentAddr
  when(io.fireTgtCycle){
    currentAddr := nextAddr
  }
  val readTagQueue = Module(new Queue(Bits(width = MEM_TAG_BITS), 16))
  val readDataQueues = Array.fill(DEVICE_WIDTH){Module(new Queue(new MemData, 16))}
  /*
  if(BURST_LENGTH - 1 > 0){
    val readDataShiftRegs = Vec.fill(BURST_LENGTH - 1){Reg(){new DDRBusNew()}}
    when(io.fireTgtCycle){
      readDataShiftRegs(0) := io.DRAMModel.readDataBus
      for(i <- 1 until BURST_LENGTH - 1){
        readDataShiftRegs(i) := readDataShiftRegs(i - 1)
      }
    }
    var readData = io.DRAMModel.readDataBus.data
    for(i <- 0 until BURST_LENGTH - 1){
      readData = Cat(readDataShiftRegs(i).data, readData)
    }
    for(i <- 0 until DEVICE_WIDTH){
      readDataQueues(i).io.enq.valid := readDataShiftRegs(BURST_LENGTH - 1 - 1).valid & io.fireTgtCycle
      readDataQueues(i).io.enq.bits := readData((i + 1)*MEM_DATA_BITS - 1, i*MEM_DATA_BITS)
    }
  } else {
    for(i <- 0 until DEVICE_WIDTH){
      readDataQueues(i).io.enq.valid := io.DRAMModel.readDataBus.valid & io.fireTgtCycle
      readDataQueues(i).io.enq.bits := io.DRAMModel.readDataBus.data((i + 1)*MEM_DATA_BITS - 1, i*MEM_DATA_BITS)
    }
  }*/
  /*io.debug_out := io.DRAMModel.readDataBus.data
  val readDataValidShiftRegs = Vec.fill(BURST_LENGTH - 1){Reg(init = Bool(false))}
  val readDataShiftRegs = Vec.fill(BURST_LENGTH - 1){Reg(init = Bits(0))}
  when(io.fireTgtCycle){
    readDataShiftRegs(0) := io.DRAMModel.readDataBus.data(0).data(DATABUS_WIDTH - 1, 0)
    readDataValidShiftRegs(0) := io.DRAMModel.readDataBus.valid
    for(i <- 1 until BURST_LENGTH - 1){
      readDataShiftRegs(i) := readDataShiftRegs(i - 1)
      readDataValidShiftRegs(i) := readDataValidShiftRegs(i - 1)
    }
  }
  val readData = Bits()
  readData := Cat(readDataShiftRegs(6), readDataShiftRegs(5), readDataShiftRegs(4), readDataShiftRegs(3), readDataShiftRegs(2), readDataShiftRegs(1), readDataShiftRegs(0), io.DRAMModel.readDataBus.data(0).data(DATABUS_WIDTH - 1, 0))
  //io.debug_readData := readData
  for(i <- 0 until DEVICE_WIDTH){
    var readDataAvail = io.fireTgtCycle & io.DRAMModel.readDataBus.valid
    for(i <- 0 until BURST_LENGTH - 1){
      readDataAvail = readDataAvail & readDataValidShiftRegs(i)
    }
    readDataQueues(i).io.enq.valid := readDataAvail
    readDataQueues(i).io.enq.bits.data := readData((i + 1)*MEM_DATA_BITS - 1, i*MEM_DATA_BITS)
  }*/
  
  //io.debug_out := io.DRAMModel.readDataBus.data
  val readDataValidShiftRegs = Vec.fill(BURST_LENGTH - 1){Reg(init = Bool(false))}
  val readDataShiftRegs = Vec.fill(BURST_LENGTH - 1){Reg(init = Bits(0))}
  when(io.fireTgtCycle){
    readDataShiftRegs(0) := io.DRAMModel.readDataBus.data
    readDataValidShiftRegs(0) := io.DRAMModel.readDataBus.valid
    for(i <- 1 until BURST_LENGTH - 1){
      readDataShiftRegs(i) := readDataShiftRegs(i - 1)
      readDataValidShiftRegs(i) := readDataValidShiftRegs(i - 1)
    }
  }
  var readData = io.DRAMModel.readDataBus.data
  for(i <- 0 until BURST_LENGTH - 1){
    readData = Cat(readDataShiftRegs(i), readData)
  }
  //io.debug_readData := readData
  for(i <- 0 until DEVICE_WIDTH){
    var readDataAvail = io.fireTgtCycle & io.DRAMModel.readDataBus.valid
    for(i <- 0 until BURST_LENGTH - 1){
      readDataAvail = readDataAvail & readDataValidShiftRegs(i)
    }
    readDataQueues(i).io.enq.valid := readDataAvail
    readDataQueues(i).io.enq.bits.data := readData((i + 1)*MEM_DATA_BITS - 1, i*MEM_DATA_BITS)
  }
  
  //read data handler(remember to check the order of the memdatas sent and received)
  val rHIdle :: rHEnq1 :: rHEnq2 :: rHEnq3 :: Nil = Enum(UInt(), 4)
  val rHCurrentState = Reg(init = rHIdle)
  val rHNextState = UInt(); rHNextState := rHCurrentState
  
  when(io.fireTgtCycle){
    rHCurrentState := rHNextState
  }
  
  io.mem_resp_queue.valid := Bool(false)
  io.mem_resp_queue.bits.tag := readTagQueue.io.deq.bits
  io.mem_resp_queue.bits.data := UInt(0)
  readTagQueue.io.deq.ready := Bool(false)
  for(i <- 0 until DEVICE_WIDTH){
    readDataQueues(i).io.deq.ready := Bool(false)
  }
  when(rHCurrentState === rHIdle){
    when(readDataQueues(0).io.deq.valid && io.mem_resp_queue.ready){
      io.mem_resp_queue.valid := Bool(true) & io.fireTgtCycle
      io.mem_resp_queue.bits.data := readDataQueues(0).io.deq.bits.data
      rHNextState := rHEnq1
    }
  }.elsewhen(rHCurrentState === rHEnq1){
    when(io.mem_resp_queue.ready){
      io.mem_resp_queue.valid := Bool(true) & io.fireTgtCycle
      io.mem_resp_queue.bits.data := readDataQueues(1).io.deq.bits.data
      rHNextState := rHEnq2
    }
  }.elsewhen(rHCurrentState === rHEnq2){
    when(io.mem_resp_queue.ready){
      io.mem_resp_queue.valid := Bool(true) & io.fireTgtCycle
      io.mem_resp_queue.bits.data := readDataQueues(2).io.deq.bits.data
      rHNextState := rHEnq3
    }
  }.elsewhen(rHCurrentState === rHEnq3){
    when(io.mem_resp_queue.ready){
      io.mem_resp_queue.valid := Bool(true) & io.fireTgtCycle
      io.mem_resp_queue.bits.data := readDataQueues(3).io.deq.bits.data
      readTagQueue.io.deq.ready := Bool(true) & io.fireTgtCycle
      for(i <- 0 until DEVICE_WIDTH){
        readDataQueues(i).io.deq.ready := Bool(true) & io.fireTgtCycle
      }
      rHNextState := rHIdle
    }
  }
    
  
  //write data delay chain
  val dataInBusValid = Bool()
  val dataInBusData = Vec.fill(DEVICE_WIDTH){new MemData}
  var writeData = dataInBusData(0).data
  for(i <- 1 until DEVICE_WIDTH){
    writeData = Cat(dataInBusData(i).data, writeData)
  }
  if(tWL + BURST_LENGTH - 1 > 0){
    val dataInBusValidRegs = Vec.fill(tWL + BURST_LENGTH - 1){Reg(init = Bool(false))}
    val dataInBusDataRegs = Vec.fill(tWL + BURST_LENGTH - 1){Reg(init = Bits(0, width = DATABUS_WIDTH))}
    io.DRAMModel.writeDataBus.valid := dataInBusValidRegs(tWL + BURST_LENGTH - 1 - 1)
    io.DRAMModel.writeDataBus.data := dataInBusDataRegs(tWL + BURST_LENGTH - 1 - 1)
    
    when(io.fireTgtCycle){
      dataInBusValidRegs(0) := Bool(false)
      for(i <- 1 until tWL + BURST_LENGTH - 1){
        dataInBusValidRegs(i) := dataInBusValidRegs(i - 1)
        dataInBusDataRegs(i) := dataInBusDataRegs(i - 1)
      }
      
      for(i <- 0 until BURST_LENGTH){
        when(dataInBusValid){
          dataInBusValidRegs(i) := dataInBusValid
          dataInBusDataRegs(i) := writeData((i+1)*DATABUS_WIDTH - 1, i*DATABUS_WIDTH)
        }
      }
    }
  } else {
    io.DRAMModel.writeDataBus.valid := dataInBusValid
    io.DRAMModel.writeDataBus.data := writeData
  }
  //control fsm
  val idle :: readRowOpened :: colOpened :: writeRowOpened :: writeData1 :: writeData2 :: writeData3 :: Nil = Enum(UInt(), 7)
  val currentState = Reg(init = idle)
  val nextState = UInt(); nextState := currentState
  when(io.fireTgtCycle){
    currentState := nextState
  }
  io.mem_cmd_queue.ready := Bool(false)
  io.mem_data_queue.ready := Bool(false)
  io.DRAMModel.cmdBus.valid := Bool(false)
  io.DRAMModel.cmdBus.cmd := activate_cmd
  io.DRAMModel.cmdBus.bankAddr := UInt(0)
  io.DRAMModel.cmdBus.rowAddr := UInt(0)
  io.DRAMModel.cmdBus.colAddr := UInt(0)
  dataInBusValid := Bool(false)
  for(i <- 0 until DEVICE_WIDTH){
    dataInBusData(i).data := Bits(0)
  }
  readTagQueue.io.enq.valid := Bool(false)
  readTagQueue.io.enq.bits := io.mem_cmd_queue.bits.tag
  val writeDatas = Vec.fill(DEVICE_WIDTH - 1){Reg(new MemData)}
  val nextWriteDatas = Vec.fill(DEVICE_WIDTH - 1){new MemData}
  for(i <- 0 until DEVICE_WIDTH - 1 ){
    when(io.fireTgtCycle){
      writeDatas(i) := nextWriteDatas(i)
    }
  }
  for(i <- 0 until DEVICE_WIDTH - 1 ){
    nextWriteDatas(i).data := writeDatas(i).data
  }
  when(currentState === idle){
    when(io.mem_cmd_queue.valid && readDataQueues(0).io.enq.ready && readDataQueues(1).io.enq.ready && readDataQueues(2).io.enq.ready && readDataQueues(3).io.enq.ready){
      when(timer.io.activate_rdy){
        //deque cmd queue
        io.mem_cmd_queue.ready := Bool(true) & io.fireTgtCycle
        //store addr
        nextAddr := io.mem_cmd_queue.bits.addr
        //set fsm outputs
        io.DRAMModel.cmdBus.valid := Bool(true)
        io.DRAMModel.cmdBus.cmd := activate_cmd
        io.DRAMModel.cmdBus.rowAddr := io.mem_cmd_queue.bits.addr(DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH)
        io.DRAMModel.cmdBus.bankAddr := io.mem_cmd_queue.bits.addr(DRAM_BANK_ADDR_WIDTH + DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH)
        when(io.mem_cmd_queue.bits.rw){
          nextState := writeRowOpened
        }.otherwise{
          readTagQueue.io.enq.valid := Bool(true) & io.fireTgtCycle
          nextState := readRowOpened
        }
      }
    }
  }.elsewhen(currentState === readRowOpened){
    when(timer.io.read_rdy){
      io.DRAMModel.cmdBus.valid := Bool(true)
      io.DRAMModel.cmdBus.cmd := read_cmd
      io.DRAMModel.cmdBus.bankAddr := currentAddr(DRAM_BANK_ADDR_WIDTH + DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH)
      io.DRAMModel.cmdBus.colAddr := currentAddr(DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, ADDR_OFFSET_WIDTH)
      nextState := colOpened
    }
  }.elsewhen(currentState === colOpened){
    when(timer.io.precharge_rdy){
      io.DRAMModel.cmdBus.valid := Bool(true)
      io.DRAMModel.cmdBus.cmd := precharge_cmd
      io.DRAMModel.cmdBus.rowAddr := currentAddr(DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH)
      io.DRAMModel.cmdBus.bankAddr := currentAddr(DRAM_BANK_ADDR_WIDTH + DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH)
      nextState := idle
    }
  }.elsewhen(currentState === writeRowOpened){
    when(io.mem_data_queue.valid){
      io.mem_data_queue.ready := Bool(true) & io.fireTgtCycle
      nextWriteDatas(0) := io.mem_data_queue.bits
      nextState := writeData1
    }
  }.elsewhen(currentState === writeData1){
    when(io.mem_data_queue.valid){
      io.mem_data_queue.ready := Bool(true) & io.fireTgtCycle
      nextWriteDatas(1) := io.mem_data_queue.bits
      nextState := writeData2
    }
  }.elsewhen(currentState === writeData2){
    when(io.mem_data_queue.valid){
      io.mem_data_queue.ready := Bool(true) & io.fireTgtCycle
      nextWriteDatas(2) := io.mem_data_queue.bits
      nextState := writeData3
    }
  }.elsewhen(currentState === writeData3){
    when(io.mem_data_queue.valid && timer.io.write_rdy){
      io.mem_data_queue.ready := Bool(true) & io.fireTgtCycle
      dataInBusValid := Bool(true)
      for(i <- 0 until DEVICE_WIDTH - 1){
        dataInBusData(i) := writeDatas(i)
      }
      dataInBusData(3) := io.mem_data_queue.bits
      io.DRAMModel.cmdBus.valid := Bool(true)
      io.DRAMModel.cmdBus.cmd := write_cmd
      io.DRAMModel.cmdBus.bankAddr := currentAddr(DRAM_BANK_ADDR_WIDTH + DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH)
      io.DRAMModel.cmdBus.colAddr := currentAddr(DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH - 1, ADDR_OFFSET_WIDTH) 
      nextState := colOpened
    }
  }
}
