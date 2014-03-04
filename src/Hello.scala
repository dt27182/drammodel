package DRAMModel 
 
import Chisel._ 
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

class Hello extends Module {
  val io = new Bundle { 
    val DRAM_errors = new DRAMModelErrorIO()
    val DRAM_tests_passed = Bool(OUTPUT)
  }
  val DRAMSystem = Module(new DRAMSystemWrapper())
  val backingMem = Module(new MockMemory())
  DRAMSystem.io.params.tRAS := UInt(4)
  DRAMSystem.io.params.tRCD := UInt(4)
  DRAMSystem.io.params.tRP := UInt(4)
  DRAMSystem.io.params.tCCD := UInt(4)
  DRAMSystem.io.params.tRTP := UInt(4)
  DRAMSystem.io.params.tWTR := UInt(4)
  DRAMSystem.io.params.tWR := UInt(4)
  DRAMSystem.io.params.tRRD := UInt(4)
  DRAMSystem.io.errors <> io.DRAM_errors

  val backingMemReqCmdQueue = Module(new Queue(new MemReqCmd(), 16))
  val backingMemReqDataQueue = Module(new Queue(new MemData(), 64))
  val backingMemRespQueue = Module(new Queue(new MemResp(), 64))
  backingMemReqCmdQueue.io.enq <> DRAMSystem.io.mem.req_cmd
  backingMemReqCmdQueue.io.deq <> backingMem.io.req_cmd
  backingMemReqDataQueue.io.enq <> DRAMSystem.io.mem.req_data
  backingMemReqDataQueue.io.deq <> backingMem.io.req_data
  backingMemRespQueue.io.enq <> backingMem.io.resp
  backingMemRespQueue.io.deq <> DRAMSystem.io.mem.resp

  DRAMSystem.io.memResp.target.ready := Bool(true)
  val mem_resp_queue = Module(new QueueFame1(64)(new MemResp()))
  val mem_req_cmd_queue = Module(new QueueFame1(64)(new MemReqCmd()))
  val mem_req_data_queue = Module(new QueueFame1(64)(new MemData()))
  DRAMSystem.io.memReqCmd <> mem_req_cmd_queue.io.deq
  DRAMSystem.io.memReqData <> mem_req_data_queue.io.deq
  DRAMSystem.io.memResp <> mem_resp_queue.io.enq
  
  val testReqs = ArrayBuffer(
    (("write", 1, 1, ((1,2,3,4)))),
    (("write", 2, 1, ((5,6,7,8)))),
    (("write", 1024, 1, ((9,10,11,12)))),
    (("write", 2025, 1, ((5,6,7,8)))),
    (("read", 1, 1, ((0,0,0,0)))),
    (("read", 2, 2, ((0,0,0,0)))),
    (("read", 1024, 3, ((0,0,0,0)))),
    (("read", 2025, 4, ((0,0,0,0))))
  )
  genDRAMSystemTestHarness2(mem_req_cmd_queue.io.enq.host_valid, mem_req_cmd_queue.io.enq.host_ready, mem_req_cmd_queue.io.enq.target.valid, mem_req_cmd_queue.io.enq.target.ready, mem_req_cmd_queue.io.enq.target.bits.rw, mem_req_cmd_queue.io.enq.target.bits.addr, mem_req_cmd_queue.io.enq.target.bits.tag, mem_req_data_queue.io.enq.host_valid, mem_req_data_queue.io.enq.host_ready, mem_req_data_queue.io.enq.target.valid, mem_req_data_queue.io.enq.target.ready, mem_req_data_queue.io.enq.target.bits.data, mem_resp_queue.io.deq.host_valid, mem_resp_queue.io.deq.host_ready, mem_resp_queue.io.deq.target.valid, mem_resp_queue.io.deq.target.ready, mem_resp_queue.io.deq.target.bits.tag, mem_resp_queue.io.deq.target.bits.data, io.DRAM_tests_passed, testReqs)
}

class HelloTests(c: Hello) extends Tester(c, Array(c.io)) {
  defTests {
    val vars = new HashMap[Node, Node]()
    val ovars = new HashMap[Node, Node]()
    var all_correct = true
    
    var done = false
    while(!done){
      all_correct = step(vars, ovars) && all_correct
      if(ovars(c.io.DRAM_tests_passed).name == "0x1"){
        done = true
      }
    }
    
    true
  }
}

object Hello {
  def main(args: Array[String]): Unit = {
    if(args(0) == "-ctest"){
      chiselMainTest(args.slice(1, args.length) ++ Array("--backend", "c", "--genHarness", "--compile", "--test", "--vcd", "--debug"), () => Module(new Hello())) {
        c => new HelloTests(c) }
    } else if(args(0) == "-vbuild"){
      chiselMain(args.slice(1,args.length) ++ Array("--backend", "v"), () => Module(new Hello()))
    } else if(args(0) == "-backannotation"){
      chiselMain(args.slice(1,args.length) ++ Array("--backend", "MyBackend.MyBackend"), () => Module(new Hello()))
    }
  }
}
