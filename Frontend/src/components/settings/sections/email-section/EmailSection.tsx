import { useFormik } from "formik";
import { useState } from "react";
import { emailSchema } from "../../../../shared/yup-schemas";
import { Button, Modal } from "antd";
import InputField from "../../../input-field/InputField";
import { Mail } from "lucide-react";
import inputFieldClasses from "../../../input-field/scss/input-field.module.css";
import classes from "./scss/email-section.module.css";
import { UserEmail } from "../../..";

const EmailSection = () => {
  const [showAddEmail, setShowAddEmail] = useState(false);
  const [isAddingEmail, setIsAddingEmail] = useState(false);

  const changeEmailFormik = useFormik({
    initialValues: {
      email: "",
    },
    validationSchema: emailSchema,
    onSubmit: async (values) => {
      setIsAddingEmail(true);
      setIsAddingEmail(false);
    },
  });

  const getEmailTag = (emailType: string) => {
    switch (emailType) {
      case "primary":
        return "default";
      case "verified":
        return "success";
      case "not_verified":
        return "warning";
      default:
        return "default";
    }
  };

  return (
    <>
      {showAddEmail && (
        <Modal
          title="Add New Email"
          open={true}
          centered={true}
          closable={!isAddingEmail}
          keyboard={!isAddingEmail}
          maskClosable={!isAddingEmail}
          okText={"Add"}
          okButtonProps={{ loading: isAddingEmail, disabled: !changeEmailFormik.isValid || !changeEmailFormik.dirty || isAddingEmail }}
          cancelButtonProps={{ disabled: isAddingEmail }}
          onOk={() => {
            changeEmailFormik.handleSubmit();
          }}
          onCancel={() => setShowAddEmail(false)}
        >
          <InputField
            icon={<Mail className={inputFieldClasses.input_icon} />}
            placeholder="Enter your email"
            name="email"
            value={changeEmailFormik.values.email}
            onChange={changeEmailFormik.handleChange}
            onBlur={changeEmailFormik.handleBlur}
            type="email"
            disabled={isAddingEmail}
            error={changeEmailFormik.touched.email && changeEmailFormik.errors.email ? changeEmailFormik.errors.email : undefined}
          />
        </Modal>
      )}
      <>
        <Button type="primary" htmlType="submit" className={classes.add_email_button} onClick={() => setShowAddEmail(true)}>
          Add Email
        </Button>
        <div className={classes.emails_list}>
          <UserEmail />
          <UserEmail />
          <UserEmail />
        </div>
      </>
    </>
  );
};

export default EmailSection;
