# bms-designer

**BMS Designer for COBOL CICS** built using **Java and OpenJFX** to visually design BMS maps and generate map source code.

This tool helps developers create **CICS BMS screen layouts visually** instead of writing BMS map definitions manually.

---

# Screenshots

### Main Editor

![Main Editor](screenshot/1.png)

### Field Editor Dialog

![Field Editor](screenshot/2.png)

### Grid Layout Editor

![Grid Layout](screenshot/3.png)

### Export Configuration

![Export Config](screenshot/4.png)

### Map Layout Example

![Map Layout](screenshot/5.png)

### Copybook Layout Example

![Copybook Layout](screenshot/6.png)

### Export File

![Export File](screenshot/7.png)

### Grid Configuration Dialog

![Generated BMS](screenshot/8.png)

### About Dialog

![Generated Copybook](screenshot/9.png)

---

# Features

* Visual **terminal grid editor**
* Default **24x80 layout**
* Add **INPUT / OUTPUT / INOUT fields**
* Drag and move fields
* Resize fields
* Field property editor
* Field name validation
* Snap-to-grid positioning
* Keyboard shortcuts
* Copy / Paste fields
* Export generated:

  * **BMS Map**
  * **COBOL Copybook**

---

# Tech Stack

* **Java**
* **OpenJFX (JavaFX)**
* Canvas-based rendering

---

# How It Works

1. Create fields on the terminal grid
2. Configure field attributes
3. Arrange the screen layout visually
4. Generate BMS map and COBOL copybook

The generated output can be used in **CICS COBOL applications**.

---

# Disclaimer

⚠️ **Learning Project**

I am currently **learning CICS and BMS map development**, so this project is mainly built for **practice and experimentation**.

Because of this:

* The generated BMS code **may not cover all edge cases**
* Some **CICS behaviors may not be fully implemented**
* The generated output **may require manual adjustments**

If you find bugs, incorrect BMS generation, or have suggestions, please **open an issue** and let me know.

Feedback and contributions are welcome.

---

# Status

🚧 Work in progress

More improvements and features will be added over time.

---

# License

This project is open source and available under the **MIT License**.
