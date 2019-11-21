; screen_demo.s
; A simple demo for our emulated screen

.org $6000
begin:

forever:
    ; we don't want to do anything outside of NMI; just wait for one to occur
    jmp forever

; A routine to draw some stuff to the screen
drawFrame:
    ldy #$00
    ldx #$00
.loop:
    tya
    sta SCREEN, x
    inx
    iny
    cpx #$10
    bne .loop
    dey
.secondloop:
    tya
    sta SCREEN, x
    dey
    inx
    cpx #$20
    bne .secondloop

    rts

brk

; Reset and interrupt service handlers
RESET:
    lda #$00
    ldx #$00
    ldy #$00
    cli     ; be sure to clear the interrupt flag, as it is set when the CPU starts up
    jmp begin

NMI:
    jsr drawFrame   ; on NMI, update graphics
    rti

.done:
    RTI

IRQ:
    RTI

; Our screen begins at $2400
.org $2400
SCREEN:

; Our CPU vectors
; This will tell the CPU where to go when an interrupt or a reset is triggered
.org $FFFA
NMI_VECTOR:
    .dw NMI
RESET_VECTOR:
    .dw RESET
IRQ_VECTOR:
    .dw IRQ
