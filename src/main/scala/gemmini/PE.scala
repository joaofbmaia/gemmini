package gemmini

import chisel3._
import chisel3.util._

class PEControl[T <: Data : Arithmetic](accType: T) extends Bundle {
  val dataflow = UInt(1.W) // TODO make this an Enum
  val propagate = UInt(1.W) // Which register should be propagated (and which should be accumulated)?
  val shift = UInt(log2Up(accType.getWidth).W) // TODO this isn't correct for Floats

}

/**
  * PE wrapper for testing
  */
class PE[T <: Data](inputType: T, outputType: T, accType: T, df: Dataflow.Value, max_simultaneous_matmuls: Int)
                   (implicit ev: Arithmetic[T]) extends Module { // Debugging variables
  import ev._

  val io = IO(new Bundle {
    val in_a = Input(inputType)
    val in_b = Input(outputType)
    val in_d = Input(outputType)
    val out_a = Output(inputType)
    val out_b = Output(outputType)
    val out_c = Output(outputType)

    val in_control = Input(new PEControl(accType))
    val out_control = Output(new PEControl(accType))

    val in_id = Input(UInt(log2Up(max_simultaneous_matmuls).W))
    val out_id = Output(UInt(log2Up(max_simultaneous_matmuls).W))

    val in_last = Input(Bool())
    val out_last = Output(Bool())

    val in_valid = Input(Bool())
    val out_valid = Output(Bool())

    val bad_dataflow = Output(Bool())
  })

  val inter_config = InterconnectConfig[T](
    verticalBroadcastType = outputType,
    horizontalBroadcastType = inputType,
    interPEType = outputType
  )

  // Which dataflow are we using?
  val OUTPUT_STATIONARY = Dataflow.OS.id.U(1.W)
  val WEIGHT_STATIONARY = Dataflow.WS.id.U(1.W)

  // Is c1 being computed on, or propagated forward (in the output-stationary dataflow)?
  val COMPUTE = 0.U(1.W)
  val PROPAGATE = 1.U(1.W)

  val mod_pe = Module(new ModPE(inter_config))

  val dataflow = io.in_control.dataflow
  val prop  = io.in_control.propagate
  val shift = io.in_control.shift
  val id = io.in_id
  val last = io.in_last
  val valid = io.in_valid

  io.out_control.dataflow := dataflow
  io.out_control.propagate := prop
  io.out_control.shift := shift
  io.out_id := id
  io.out_last := last
  io.out_valid := valid

  val last_s = RegEnable(prop, valid)
  val flip = last_s =/= prop
  val shift_offset = Mux(flip, shift, 0.U)



  mod_pe.io.in_v_bcast := io.in_b
  mod_pe.io.in_h_bcast := io.in_a
  io.out_b := mod_pe.io.out_v_bcast
  io.out_a := mod_pe.io.out_h_bcast
  mod_pe.io.in_v := io.in_d
  mod_pe.io.in_h := 0.S
  mod_pe.io.in_d := 0.S
  io.out_c := mod_pe.io.out
  mod_pe.io.valid := valid

  val OS_control_set = Wire(new ModPEControl(inter_config))
  OS_control_set.sel_a := PEMuxSel.H_BCAST
  OS_control_set.sel_b := PEMuxSel.V_BCAST
  OS_control_set.sel_c := PEMuxSel.REG
  OS_control_set.sel_q := PEMuxSel.V
  OS_control_set.fu_control.big_alu_sel := BigALUSel.MUL
  OS_control_set.fu_control.minus_a := false.B
  OS_control_set.fu_control.minus_m := false.B
  OS_control_set.fu_control.sel_m := MMuxSel.BIG_ALU
  OS_control_set.fu_control.shift := shift_offset
  OS_control_set.fu_control.small_alu_sel := SmallALUSel.ADD
  OS_control_set.use_double_buffer := true.B
  OS_control_set.double_buffer_sel := (prop === PROPAGATE)

  //does not work :)
  val WS_control_set = Wire(new ModPEControl(inter_config))
  WS_control_set.sel_a := PEMuxSel.H_BCAST
  WS_control_set.sel_b := PEMuxSel.REG
  WS_control_set.sel_c := PEMuxSel.V
  WS_control_set.sel_q := PEMuxSel.V
  WS_control_set.fu_control.big_alu_sel := BigALUSel.MUL
  WS_control_set.fu_control.minus_a := false.B
  WS_control_set.fu_control.minus_m := false.B
  WS_control_set.fu_control.sel_m := MMuxSel.BIG_ALU
  WS_control_set.fu_control.shift := shift_offset
  WS_control_set.fu_control.small_alu_sel := SmallALUSel.ADD
  WS_control_set.use_double_buffer := true.B
  WS_control_set.double_buffer_sel := (prop === PROPAGATE)


  io.bad_dataflow := false.B
  when ((df == Dataflow.OS).B || ((df == Dataflow.BOTH).B && dataflow === OUTPUT_STATIONARY)) {
    mod_pe.io.control := OS_control_set
  }.elsewhen ((df == Dataflow.WS).B || ((df == Dataflow.BOTH).B && dataflow === WEIGHT_STATIONARY)) {
    mod_pe.io.control := WS_control_set
  }.otherwise {
    mod_pe.io.control := OS_control_set
    io.bad_dataflow := true.B
  }

}
