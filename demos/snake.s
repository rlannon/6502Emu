;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;       6502 Snake      ;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; A simple snake game written for the emulated 6502 processor of mine
;; Adapted from skilldrick's version: https://skilldrick.github.io/easy6502/#snake

;; Define some macros

; Macros for our keys, inputs
.macro KEY_UP $26
.macro KEY_DOWN $28
.macro KEY_LEFT $25
.macro KEY_RIGHT $27
.macro SCREEN $2400     ; the screen is located at $2400 - $27FF -- each row is $20 bytes long
.macro START_POS $2600  ; The snake's start address
.macro KEYBOARD $F000   ; The keyboard input

; macros to hold our directions (each uses a separate bit)
.macro MOVE_UP $01
.macro MOVE_DOWN $02
.macro MOVE_RIGHT $04
.macro MOVE_LEFT $08

; Some variables
.rsset $0000    ; RAM should begin at address $00
; pointer to the apple
.rs 1 apple_low
.rs 1 apple_high

.rs 1 direction ; 1 = up; 2 = down; 4 = right; 8 = left
.rs 1 snake_length  ; snake length in terms of _bytes of memory_ (so a value of $04 => 4 bytes => 2 pixels)
.rs 1 input     ; the last fetched input from the user
.rs 1 draw_ready    ; whether we are ready to draw
.rs 1 draw_done ; whether we have finished drawing

.rs 1 frame_count   ; track the number of frames that have passed since it was last cleared
; the seed for our PRNG
.rs 1 seed_low
.rs 1 seed_high
.rs 1 temp

; The snake data - begin at $0010
; Note that these values are to be used with indirect addressing (they're pointers)
.macro snake_head_low $10   ; the low byte of the address of the snake's head
.macro snake_head_high $11  ; the high byte of the address of the snake's head
.macro snake_body_start $12 ; the beginning of the snake's body
.macro RAND $ff


;; The implementation

; The main game loop
.org $6000
gameloop:
    ; update every third frame
    lda frame_count
    cmp #$03
    bcc .forever

    jsr check_collision ; check for a collision with the snake and the apple or with the snake and itself
    jsr update_snake    ; update the snake
    jsr draw_apple
    lda #$01
    sta draw_ready

    ; finally, set our draw_done flag to 0
    ; this indicates to our loop that we shouldn't run until the next graphics update is finished
    lda #$00
    sta draw_done
    sta frame_count
;; this will continue looping until an NMI is triggered
.forever:
    lda draw_done   ; if (draw_done != 0)
    cmp #$00        ;
    bne gameloop    ; run the game loop
    ; if we haven't updated yet, continue adding to the seed
    lda seed_low
    adc #$01
    bcc .seed_done
    inc seed_high
.seed_done:
    jmp .forever    ; else, jump to gameloop.forever to keep looping until an NMI sets the done flag

;; Some subroutines
; todo: write subroutines for game operations

;; check for a collision
check_collision:
    jsr check_apple_collision
    jsr check_snake_collision
    rts

;; check for collision with apple
check_apple_collision:
    ; compare low bytes
    lda snake_head_low
    cmp apple_low
    bne .done
    ; compare high bytes
    lda snake_head_high
    cmp apple_high
    bne .done

    ; if we get here, we need to eat the apple
    inc snake_length
    inc snake_length        ; increase the snake length (remember it's number of _bytes_, not number of pixels)

    ; generate a new apple
    jsr gen_apple_position  ; generate a new apple
.done:
    rts

;; check for collision with snake
check_snake_collision:
    ldx #$02    ; start with second segment
.loop:
    lda snake_head_low, x
    cmp snake_head_low  ; see if the head is in the same position as the segment we are examining
    bne .continue
.maybe_collided:
    ; check the high bytes to see if they are in the same position
    lda snake_head_high, x
    cmp snake_head_high
    beq .collided   ; if the high bytes are also the same, they collided
.continue:
    inx
    inx
    cpx snake_length    ; if we have looped through the entire length
    beq .done
    jmp .loop   ; continue looping through
.collided:
    jmp game_over   ; on a collision, it's game over
.done:
    rts

;; Generate a new random position for the apple
gen_apple_position:
    ; a random byte can go into the position's low byte
    jsr gen_random  ; load a random number into A
    sta apple_low

    ; the high byte must be between $24 and $27
    ; so first, generate a number between 0 and 3 by masking out the two lowest bits
    jsr gen_random
    and #$03

    ; then, add $24 to it to make it in the range $24 - $27
    clc
    adc #$24
    sta apple_high

    ; done
    rts

;; Update the snake position, checking for illegal collisions
update_snake:
    ldx snake_length    ; get the snake length; decrement by 1 because we are indexing an array
    dex
.loop:
    lda snake_head_low, x   ; get one byte of the snake's tail
    sta snake_body_start, x
    dex ; go to second-to-last, third-to-last, etc.
    bpl .loop   ; branch on N=0 (until X=$FF)

    ; we have to update the high bytes based on the snake's direction
    lda direction
    lsr a
    bcs .up
    lsr a
    bcs .down
    lsr a
    bcs .right
    lsr a
    bcs .left
    rts
.up:
    ; if we are moving up
    lda snake_head_low
    sec
    sbc #$20    ; subtract 32 (the number of pixels per line) from the low byte
    sta snake_head_low
    bcc .upup    ; if a borrow occurred, we need to subtract one from the high byte
    rts
.upup:
    dec snake_head_high ; perform the borrow
    lda #$23
    cmp snake_head_high ; if (snake_head_high == $23)
    beq .collision      ; jmp .collision
    rts
.right:
    ; if we are going right, we need to add 1 to the low byte
    inc snake_head_low
    lda #$1f
    bit snake_head_low  ; if (snake_head_low == $1f) -> bit will set the Z flag if snake_head_low is $20, and we will branch
    beq .collision      ; jmp .collision
    rts
.down:
    ; if we are moving down, we need to add $20 to the low byte
    lda snake_head_low
    clc
    adc #$20
    sta snake_head_low
    bcs .downdown   ; if there is a carry, we need to do it
    rts
.downdown:
    inc snake_head_high
    lda #$28    ; if we hit the bottom wall, the high byte will be $28 (screen is $2400 to $27FF)
    cmp snake_head_high
    beq .collision
    rts
.left:
    ; if we are moving left, we need to subtract one from the low byte
    dec snake_head_low
    lda snake_head_low
    and #$1f    ; if we go past the edge, the byte will be $FF, $1F, $3F, etc.
    cmp #$1f    ; so AND #$1F will give us $1F if we are out of bounds
    beq .collision
    rts
.collision:
    ; on a collision, it's game over
    jmp game_over

;; Draw Routines

;; draw the updated snake
draw_snake:
    ; erase the end of the snake by performing a lookup within our pointer table
    ldx snake_length
    lda #$00
    sta (snake_head_low, x)

    ; paint the head
    ldx #$00
    lda #$01
    sta (snake_head_low, x)
    rts

;; make the apple a random color
draw_apple:
    jsr gen_random  ; the random position is now in A
    and #$0F
    bne .draw   ; if A is not in the range of $01 to $0F, set it to 5 (apple shouldn't be black)
    lda #$05
.draw:
    ldy #$00
    sta (apple_low), y  ; store it in the address pointed to by the apple variable
    rts

;; the game over routine
game_over:
    ; loop forever, do nothing
    lda #$FF
    ldx #$FF
    ldy #$FF
    jmp game_over

;; generate a random number based on user input and number of frames since last button press
;; the random number will be returned via the A register
gen_random:
    ; A simple linear feedback shift register
    ldy #$08    ; 8 iterations
    lda seed_low
.first:
    asl a
    rol seed_high
    bcc .second
    eor #$39    ; whenever a 1 is shifted out, apply XOR feedback
.second:
    dey
    bne .first
    sta seed_low
    cmp #$00
    rts

;; Update the direction based on the key press
update_direction:
    lda KEYBOARD
    
    cmp #KEY_UP
    beq .up
    cmp #KEY_DOWN
    beq .down
    cmp #KEY_RIGHT
    beq .right
    cmp #KEY_LEFT
    beq .left
    ; if we didn't have any valid direction, do nothing
    rts
.up:
    lda #MOVE_DOWN
    bit direction
    bne .illegal_move

    lda #MOVE_UP
    jmp .done
.down:
    lda #MOVE_UP
    bit direction
    bne .illegal_move

    lda #MOVE_DOWN
    jmp .done
.right:
    lda #MOVE_LEFT
    bit direction
    bne .illegal_move

    lda #MOVE_RIGHT
    jmp .done
.left:
    lda #MOVE_RIGHT
    bit direction
    bne .illegal_move

    lda #MOVE_LEFT
    jmp .done
.done:
    ; update the direction and return
    sta direction
.illegal_move:
    rts

; Our vectors
.org $E000

RESET:
    ; clear our video memory and our zero page
    ldx #$00
    lda #$00
.clear_zero:
    sta $00, x
    inx
    cpx #$00
    bne .clear_zero

    lda #$24
    sta $01
    lda #$00
    sta $00

.clear_screen_init:
    lda #$00
    ldy #$00
.clear_screen:
    sta ($00), y
    iny
    cpy #$00
    bne .clear_screen
    inc $01
    lda $01
    cmp #$28
    bne .clear_screen_init

    lda #$00
    sta $01

    ; initialize the game state
    lda #MOVE_UP
    sta direction   ; begin with the snake moving up

    lda #KEY_UP
    sta KEYBOARD    ; store the value for up in the keyboard address so that the direction is valid

    lda #$04    ; 2 pixels wide to start (4 memory addresses)
    sta snake_length

    ;; start in the middle of the screen
    ; Low bytes
    lda #$11
    sta snake_head_low  ; x axis

    ; the head starts at $2611, so the first segment is at $2610
    lda #$10
    sta snake_body_start    ; body segment 1, low
    lda #$0f
    sta $14 ; location of body segment 2, low

    ; high bytes
    lda #$26
    sta snake_head_high ; y axis
    sta $13 ; location of body segment 1, high
    sta $15 ; location of body segment 2, high

    ; seed our number generator
    lda #$C0
    sta seed_low
    lda #$23
    sta seed_high   ; the apple will start in the same place, but since we use user input and the number of executed
                    ; instructions to seed the PRNG, subsequent positions will be different in each game

    ;; generate first apple
    jsr gen_apple_position
    jsr draw_apple
    jsr draw_snake

    ; clear our frame count
    lda #$03
    sta frame_count
    sta draw_ready

    cli     ; clear the interrupt flag, as it is set on system reset
    JMP gameloop    ; go to our game loop

NMI:
    ; save our registers
    pha
    txa
    pha
    tya
    pha

    ; fetch input from the user -- see if we need
    lda direction   ; get the current direction
    sta temp
    jsr update_direction    ; get the new direction
    cmp temp
    beq .draw_graphics
    ; reset the frame count
    lda frame_count
    sta seed_high
    lda #$00
    sta seed_low
    sta frame_count
.draw_graphics:
    ; update our graphics
    jsr draw_snake

    inc draw_done   ; set draw_done equal to some value other than 0 as we are ready to run the main loop again
    inc frame_count

    ; return our register values
    pla
    tay
    pla
    tax
    pla

    ; return from the interrupt
    rti

;; we won't use regular interrupts in this program as the user inputs are polled
IRQ:
    rti


; Our vectors
; These define where the processor will look to jump to when it gets various signals
.org $FFFA
NMI_VECTOR:
    .dw NMI
RESET_VECTOR:
    .dw RESET
IRQ_VECTOR:
    .dw IRQ
