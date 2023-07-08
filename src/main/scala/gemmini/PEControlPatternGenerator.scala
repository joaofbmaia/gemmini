package gemmini

import chisel3._
import chisel3.util._

class ControlPatternTableLine[T <: Data](interconnectConfig : InterconnectConfig[T], controlPatternTableSize: Int) extends Bundle {
  val pe_control = new ModPEControl(interconnectConfig)
  val repeat_for_n_cycles = UInt(8.W)
  val next_index = UInt(log2Ceil(controlPatternTableSize).W)
}

class PEControlPatternGenerator[T <: Data](interconnectConfig : InterconnectConfig[T], controlPatternTableSize: Int) extends Module { 
  val io = IO(new Bundle {
    val valid = Input(Bool())
    
    // which control pattern to use
    val next_control_pattern_index = Input(UInt(log2Ceil(controlPatternTableSize).W))

    // true when the control pattern to use changes
    val new_control_pattern = Input(Bool())

    // output
    val pe_control = Output(new ModPEControl(interconnectConfig))

    // reconfiguration
    val write_enable = Input(Bool())
    val write_index = Input(UInt(log2Ceil(controlPatternTableSize).W))
    val write_data = Input(new ControlPatternTableLine(interconnectConfig, controlPatternTableSize))
  })

  // control pattern table index
  val control_pattern_table_index = RegInit(0.U(log2Ceil(controlPatternTableSize).W))

  // control pattern table
  val control_pattern_table = Mem(controlPatternTableSize, new ControlPatternTableLine(interconnectConfig, controlPatternTableSize))
  val current_line = control_pattern_table(control_pattern_table_index)

  val new_index = Wire(Bool())

  // control pattern table index
  when(io.new_control_pattern) {
    control_pattern_table_index := io.next_control_pattern_index
  }.elsewhen(new_index) {
    control_pattern_table_index := current_line.next_index
  }

  // counter
  val counter = RegInit(0.U(8.W))
  when(io.new_control_pattern || new_index) {
    counter := 0.U
  }.elsewhen(io.valid && !io.write_enable) {
    counter := counter + 1.U
  }

  // Comparator
  new_index := current_line.repeat_for_n_cycles === counter && !io.write_enable

  // Outputs
  io.pe_control := current_line.pe_control

  // Reconfiguration
  when(io.write_enable) {
    control_pattern_table(io.write_index) := io.write_data
  }
}
