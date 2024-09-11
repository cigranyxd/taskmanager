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
    public partial class frmRunTask : Form
    {
        public frmRunTask()
        {
            InitializeComponent();
        }

        private void runBtn_Click(object sender, EventArgs e)
        {
            if(!string.IsNullOrEmpty(runTextB.Text) ) {
                try
                {
                    Process proc = new Process();
                    proc.StartInfo.FileName = runTextB.Text;
                    proc.Start();
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.Message, "Message", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            
            }
        }
    }
}
