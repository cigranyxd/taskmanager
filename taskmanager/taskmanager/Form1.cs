using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace taskmanager
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
            
        }

        Process[] proc;

        void getAllProcesses()
        {
            proc = Process.GetProcesses();
            listBox1.Items.Clear();
            foreach (Process process in proc)
            {
                listBox1.Items.Add(process.ProcessName);
            }
        }

        private void menuStrip1_ItemClicked(object sender, ToolStripItemClickedEventArgs e)
        {

        }

        private void Form1_Load(object sender, EventArgs e)
        {
            getAllProcesses();
        }

        private void endTaskBtn_Click(object sender, EventArgs e)
        {
            try
            {
                proc[listBox1.SelectedIndex].Kill();
                getAllProcesses();
            }
            catch (Exception ex)
            {

                MessageBox.Show(ex.Message, "Message", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }
        private void runTaskToolStripMenuItem_Click(object sender, EventArgs e)
        {
            using (frmRunTask frm = new frmRunTask())
            {
                if (frm.ShowDialog() == DialogResult.OK)
                {
                    getAllProcesses();
                }
            }
        }

        private void calendarToolStripMenuItem_Click(object sender, EventArgs e)
        {
            using (frmCalendar frm = new frmCalendar()) { 
            frm.ShowDialog();
            }
        }
    }
}
