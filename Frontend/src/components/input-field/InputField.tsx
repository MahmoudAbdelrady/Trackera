import { Form, Input } from "antd";
import classes from "./scss/input-field.module.css";

interface InputFieldProps {
  label?: string;
  icon?: React.ReactNode;
  type: string;
  name: string;
  placeholder?: string;
  value?: string;
  onChange?: (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onBlur?: (event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  error?: string;
  disabled?: boolean;
}

const InputField = (props: InputFieldProps) => {
  const { label, icon, type, name, placeholder, value, onChange, onBlur, error, disabled } = props;

  const fieldProps = {
    prefix: <div className={classes.input_icon}>{icon}</div>,
    placeholder,
    className: classes.input_field,
    name,
    value,
    onChange,
    onBlur,
    disabled,
    type,
  };

  const handlePreventPasswordCopyPaste = (event: React.ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault();
  };

  return (
    <div className={classes.input_group}>
      {label && <div className={classes.input_label}>{label}</div>}

      <Form.Item
        style={{ marginBottom: 0, width: "100%" }}
        validateStatus={error ? "error" : ""}
        help={error ? error : ""}
      >
        {type === "password" ? (
          <Input.Password
            {...fieldProps}
            onCut={handlePreventPasswordCopyPaste}
            onCopy={handlePreventPasswordCopyPaste}
            onPaste={handlePreventPasswordCopyPaste}
          />
        ) : type === "textarea" ? (
          <Input.TextArea {...fieldProps} prefix={undefined} rows={5} />
        ) : (
          <Input {...fieldProps} />
        )}
      </Form.Item>
    </div>
  );
};

export default InputField;
