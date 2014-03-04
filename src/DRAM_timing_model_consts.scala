package DRAMModel

import Chisel._
import scala.math._

object MemModelConstants{
  val NUM_BANKS = 8
  val NUM_ROWS = 32768
  val NUM_COLS = 512
  val DATABUS_WIDTH = 64
  val ROW_WIDTH = 512
  val PADDR_BITS = 40
  val MEM_DATA_BITS = 128
  val MEM_TAG_BITS = 5
  val OFFSET_BITS = 6
  val BURST_LENGTH = ROW_WIDTH/DATABUS_WIDTH
  val DEVICE_WIDTH = ROW_WIDTH/MEM_DATA_BITS//row width in terms of MemData width
  Predef.assert(DEVICE_WIDTH == 4)
  val DRAM_BANK_ADDR_WIDTH = log2Up(NUM_BANKS)
  val DRAM_ROW_ADDR_WIDTH = log2Up(NUM_ROWS)
  val DRAM_COL_ADDR_WIDTH = log2Up(NUM_COLS)
  val ADDR_OFFSET_WIDTH = if(DEVICE_WIDTH == 4) 0 else log2Up(DEVICE_WIDTH/4)

  Predef.assert(DRAM_BANK_ADDR_WIDTH + DRAM_ROW_ADDR_WIDTH + DRAM_COL_ADDR_WIDTH + ADDR_OFFSET_WIDTH < PADDR_BITS - OFFSET_BITS)
  val TIMING_COUNTER_WIDTH = 8
  val tCL = 4
  Predef.assert(tCL > 0)
  val tWL = 3
  Predef.assert(tWL > 0)
  val NUM_CMDS = 5
  val activate_cmd :: read_cmd :: write_cmd :: precharge_cmd :: other_cmd :: Nil=Enum(UInt(), NUM_CMDS)
  val CMD_WIDTH = log2Up(NUM_CMDS)
}
