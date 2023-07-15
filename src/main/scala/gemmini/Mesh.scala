package gemmini

import chisel3._
import chisel3.util._
import chisel3.experimental._


class Mesh[T <: Data: Arithmetic]
  (interconnectConfig : InterconnectConfig[T], controlPatternTableSize: Int,
   meshRows: Int, meshColumns: Int)
  extends Module {

  assert(meshRows == meshColumns)
  val block_size = meshRows


  val io = IO(new Bundle {
    val in_h_grid = Input(Vec(meshRows, interconnectConfig.horizontalGridType))
    val in_v_grid = Input(Vec(meshColumns, interconnectConfig.verticalGridType))
    val in_v = Input(Vec(meshColumns, interconnectConfig.interPEType))
    val in_h = Input(Vec(meshRows, interconnectConfig.interPEType))

    val out_h_grid = Output(Vec(meshRows, interconnectConfig.horizontalGridType)) // do i need this?
    val out_v_grid = Output(Vec(meshColumns, interconnectConfig.verticalGridType)) //do i need this?
    val out = Output(Vec(meshColumns, interconnectConfig.interPEType))

    val valid = Input(Bool())
    // mesh is always ready :)


    // which configuration pattern to use
    val next_control_pattern_indexes = Input(Vec(meshRows, Vec(meshColumns, UInt(log2Ceil(controlPatternTableSize).W))))

    // true when the configuration pattern to use changes
    val new_control_pattern = Input(Vec(meshRows, Vec(meshColumns, Bool())))

    // reconfiguration
    val rcfg = Input(new TableReconfigurationControl(controlPatternTableSize, 1))
    val rcfg_active = Input(Bool())
  })

  val valid_cycle = Wire(Bool())
  valid_cycle := io.valid && !io.rcfg_active

  // mesh(r)(c) => Tile at row r, column c
  val mesh: Seq[Seq[PE[T]]] = Seq.fill(meshRows, meshColumns)(Module(new PE(interconnectConfig, controlPatternTableSize)))
  //val meshT = mesh.transpose

  def pipe[T <: Data](valid: Bool, t: T, latency: Int): T = {
    // The default "Pipe" function apparently resets the valid signals to false.B. We would like to avoid using global
    // signals in the Mesh, so over here, we make it clear that the reset signal will never be asserted
    chisel3.withReset(false.B) { Pipe(valid, t, latency).bits }
  }

  // connect horizontal inputs / outputs
  for (r <- 0 until meshRows) {
    mesh(r)(0).io.in_h_grid := io.in_h_grid(r)
    mesh(r)(0).io.in_h := io.in_h(r)
    mesh(r)(0).io.in_d := 0.U.asTypeOf(interconnectConfig.interPEType)
    io.out_h_grid(r) := pipe(valid_cycle, mesh(r).last.io.out_h_grid, 1) //do i need this?
  }

  // connect vertical inputs / outputs
  for (c <- 0 until meshColumns) {
    mesh(0)(c).io.in_v_grid := io.in_v_grid(c)
    mesh(0)(c).io.in_v := io.in_v(c)
    if (c > 0) mesh(0)(c).io.in_d := 0.U.asTypeOf(interconnectConfig.interPEType)
    io.out_v_grid(c) := pipe(valid_cycle, mesh.last(c).io.out_v_grid, 1) //do i need this?
    io.out(c) := pipe(valid_cycle, mesh.last(c).io.out, 1)
  }

  for (r <- 0 until meshRows) {
    for (c <- 0 until meshColumns) {
      // data lines
      if (r > 0) mesh(r)(c).io.in_v_grid := Mux(io.rcfg_active, pipe(io.rcfg_active, mesh(r - 1)(c).io.out_v_grid, 1), pipe(valid_cycle, mesh(r - 1)(c).io.out_v_grid, 1)) // V_GRID and configuration
      if (r > 0) mesh(r)(c).io.in_v := pipe(valid_cycle, mesh(r - 1)(c).io.out, 1) // V
      if (c > 0) mesh(r)(c).io.in_h_grid := pipe(valid_cycle, mesh(r)(c - 1).io.out_h_grid, 1) // H_GRID
      if (c > 0) mesh(r)(c).io.in_h := pipe(valid_cycle, mesh(r)(c - 1).io.out, 1) // H
      if (r > 0 && c > 0) mesh(r)(c).io.in_d := pipe(valid_cycle, mesh(r - 1)(c - 1).io.out, 1) // D

      mesh(r)(c).io.next_control_pattern_index := io.next_control_pattern_indexes(r)(c)
      mesh(r)(c).io.new_control_pattern := io.new_control_pattern(r)(c)
      mesh(r)(c).io.in_valid := valid_cycle
      mesh(r)(c).io.rcfg := io.rcfg
    }
  }
}

